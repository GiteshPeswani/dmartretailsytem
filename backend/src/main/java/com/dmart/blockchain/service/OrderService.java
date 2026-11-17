package com.dmart.blockchain.service;

import com.dmart.blockchain.blockchain.BlockchainService;
import com.dmart.blockchain.blockchain.HashUtil;
import com.dmart.blockchain.dto.request.OrderCreateRequest;
import com.dmart.blockchain.dto.response.OrderItemResponse;
import com.dmart.blockchain.dto.response.OrderResponse;
import com.dmart.blockchain.dto.response.VerificationResponse;
import com.dmart.blockchain.entity.*;
import com.dmart.blockchain.entity.BlockchainTransaction.EntityType;
import com.dmart.blockchain.exception.BadRequestException;
import com.dmart.blockchain.exception.InsufficientStockException;
import com.dmart.blockchain.exception.InvalidStatusTransitionException;
import com.dmart.blockchain.exception.ResourceNotFoundException;
import com.dmart.blockchain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final InventoryMovementRepository movementRepository;
    private final AuditEventRepository auditEventRepository;
    private final BlockchainService blockchainService;
    private final BlockchainTransactionRepository bcTxRepo;

    @Transactional
    public OrderResponse checkout(String userEmail, OrderCreateRequest req) {
        User customer = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + userEmail));

        List<CartItem> cartItems = cartItemRepository.findByUserIdWithProduct(customer.getId());
        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cannot checkout with an empty cart");
        }

        // Validate stock for all items
        for (CartItem ci : cartItems) {
            Product p = ci.getProduct();
            if (!p.isActive()) {
                throw new BadRequestException("Product " + p.getName() + " is currently inactive");
            }
            if (p.getStockQuantity() < ci.getQuantity()) {
                throw new InsufficientStockException("Insufficient stock for " + p.getName() +
                    ". Available: " + p.getStockQuantity() + ", In cart: " + ci.getQuantity());
            }
        }

        // Generate unique order number
        String orderNumber = "DMART-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 900 + 100);

        Order order = Order.builder()
            .orderNumber(orderNumber)
            .customer(customer)
            .deliveryAddress(req.getDeliveryAddress().trim())
            .notes(req.getNotes())
            .status(OrderStatus.PENDING)
            .subtotal(BigDecimal.ZERO)
            .discountAmount(BigDecimal.ZERO)
            .totalAmount(BigDecimal.ZERO)
            .blockchainStatus(BlockchainStatus.NOT_SUBMITTED)
            .build();

        BigDecimal subtotal = BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal grandTotal = BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem ci : cartItems) {
            Product p = ci.getProduct();
            BigDecimal unitPrice = p.getPrice().setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal effectivePrice = p.getEffectivePrice();
            BigDecimal lineTotal = effectivePrice.multiply(BigDecimal.valueOf(ci.getQuantity()))
                .setScale(2, java.math.RoundingMode.HALF_UP);

            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(ci.getQuantity())))
                .setScale(2, java.math.RoundingMode.HALF_UP);
            grandTotal = grandTotal.add(lineTotal).setScale(2, java.math.RoundingMode.HALF_UP);

            // Deduct stock and record movement
            int qtyBefore = p.getStockQuantity();
            int qtyAfter = qtyBefore - ci.getQuantity();
            p.setStockQuantity(qtyAfter);
            productRepository.save(p);

            movementRepository.save(InventoryMovement.builder()
                .product(p)
                .user(customer)
                .movementType(InventoryMovement.MovementType.SALE)
                .quantityChange(-ci.getQuantity())
                .quantityBefore(qtyBefore)
                .quantityAfter(qtyAfter)
                .reason("Customer Order: " + orderNumber)
                .build());

            OrderItem oi = OrderItem.builder()
                .order(order)
                .product(p)
                .productSku(p.getSku())
                .productName(p.getName())
                .quantity(ci.getQuantity())
                .unitPrice(unitPrice)
                .discountPercent(p.getDiscountPercent())
                .lineTotal(lineTotal)
                .build();
            orderItems.add(oi);
        }

        order.setItems(orderItems);
        order.setSubtotal(subtotal);
        order.setDiscountAmount(subtotal.subtract(grandTotal).setScale(2, java.math.RoundingMode.HALF_UP));
        order.setTotalAmount(grandTotal);

        // Precompute canonical state hash
        String orderHash = HashUtil.toHex(HashUtil.hashOrder(order));
        order.setOrderHash(orderHash);

        Order saved = orderRepository.save(order);

        // Clear cart
        cartItemRepository.deleteByUserId(customer.getId());

        // Create initial AuditEvent
        auditEventRepository.save(AuditEvent.builder()
            .entityType(EntityType.ORDER)
            .entityId(saved.getId())
            .eventType("ORDER_CREATED")
            .actor(customer)
            .actorRole("CUSTOMER")
            .description("Order " + saved.getOrderNumber() + " placed with " + orderItems.size() + " items.")
            .build());

        log.info("Order created successfully: {} (ID: {})", saved.getOrderNumber(), saved.getId());

        // Synchronously register on blockchain so order is immediately confirmed and verified
        try {
            blockchainService.createOrderOnChainSync(saved);
        } catch (Exception ex) {
            log.warn("Synchronous blockchain registration fallback: {}", ex.getMessage());
            blockchainService.createOrderOnChainAsync(saved);
        }

        Order freshSaved = orderRepository.findById(saved.getId()).orElse(saved);
        return toResponse(freshSaved);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getCustomerOrders(String userEmail, Pageable pageable) {
        User customer = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));
        return orderRepository.findByCustomerId(customer.getId(), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse getCustomerOrderById(String userEmail, Long orderId) {
        User customer = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userEmail));

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        // Isolate customer access
        if (!order.getCustomer().getId().equals(customer.getId()) &&
            customer.getRoles().stream().noneMatch(r -> r.getName().equals(Role.ADMIN) || r.getName().equals(Role.STORE_MANAGER))) {
            throw new AccessDeniedException("You are not authorized to view this order");
        }

        return toResponse(order);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrdersAdmin(OrderStatus status, String search, Pageable pageable) {
        return orderRepository.findAllWithFilters(status, search, pageable).map(this::toResponse);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, OrderStatus newStatus, String reason, String actorEmail) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        OrderStatus currentStatus = order.getStatus();
        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(
                "Cannot transition order status from " + currentStatus + " to " + newStatus);
        }

        User actor = userRepository.findByEmail(actorEmail).orElse(null);
        String actorRole = actor != null && !actor.getRoles().isEmpty()
            ? actor.getRoles().iterator().next().getName() : "SYSTEM";

        // If transitioning to CANCELLED, restore inventory
        if (newStatus == OrderStatus.CANCELLED) {
            for (OrderItem oi : order.getItems()) {
                Product p = oi.getProduct();
                int qtyBefore = p.getStockQuantity();
                int qtyAfter = qtyBefore + oi.getQuantity();
                p.setStockQuantity(qtyAfter);
                productRepository.save(p);

                movementRepository.save(InventoryMovement.builder()
                    .product(p)
                    .user(actor)
                    .order(order)
                    .movementType(InventoryMovement.MovementType.RETURN)
                    .quantityChange(oi.getQuantity())
                    .quantityBefore(qtyBefore)
                    .quantityAfter(qtyAfter)
                    .reason("Order Cancellation: " + order.getOrderNumber() + (reason != null ? " - " + reason : ""))
                    .build());
            }
        }

        order.setStatus(newStatus);
        Order updated = orderRepository.save(order);

        // Record audit event
        auditEventRepository.save(AuditEvent.builder()
            .entityType(EntityType.ORDER)
            .entityId(updated.getId())
            .eventType("STATUS_CHANGE_" + newStatus.name())
            .actor(actor)
            .actorRole(actorRole)
            .description("Order status changed from " + currentStatus + " to " + newStatus +
                (reason != null ? ". Reason: " + reason : ""))
            .build());

        // Asynchronously send status update event to blockchain
        blockchainService.updateOrderStatusOnChainAsync(updated, newStatus);

        log.info("Order {} status changed from {} to {}", updated.getOrderNumber(), currentStatus, newStatus);
        return toResponse(updated);
    }

    @Transactional(readOnly = true)
    public VerificationResponse verifyOrderOnChain(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        String localHash = HashUtil.toHex(HashUtil.hashOrder(order));
        Long blockNumber = bcTxRepo.findTopByEntityTypeAndEntityIdOrderBySubmittedAtDesc(
            BlockchainTransaction.EntityType.ORDER, orderId)
            .map(BlockchainTransaction::getBlockNumber)
            .orElse(null);

        if (!blockchainService.isConnected()) {
            return VerificationResponse.builder()
                .verified(false)
                .blockchainAvailable(false)
                .entityType("ORDER")
                .entityId(orderId)
                .localHash(localHash)
                .onChainHash(order.getOrderHash())
                .status("UNKNOWN")
                .isTampered(false)
                .message("Blockchain node is currently offline or unreachable.")
                .txHash(order.getBlockchainTxHash())
                .blockNumber(blockNumber)
                .build();
        }

        BlockchainService.VerificationResult result = blockchainService.verifyOrderOnChain(order);
        if (!result.isAvailable()) {
            return VerificationResponse.builder()
                .verified(false)
                .blockchainAvailable(false)
                .entityType("ORDER")
                .entityId(orderId)
                .localHash(localHash)
                .onChainHash(order.getOrderHash())
                .status("ERROR")
                .isTampered(false)
                .message(result.message())
                .txHash(order.getBlockchainTxHash())
                .blockNumber(blockNumber)
                .build();
        }

        boolean isTampered = !result.isVerified();
        String status = result.isVerified() ? "VERIFIED" : "TAMPERED";

        Order refreshed = orderRepository.findById(orderId).orElse(order);
        Long updatedBlockNumber = bcTxRepo.findTopByEntityTypeAndEntityIdOrderBySubmittedAtDesc(
            BlockchainTransaction.EntityType.ORDER, orderId)
            .map(BlockchainTransaction::getBlockNumber)
            .orElse(blockNumber);

        return VerificationResponse.builder()
            .verified(result.isVerified())
            .blockchainAvailable(result.isAvailable())
            .entityType("ORDER")
            .entityId(orderId)
            .localHash(localHash)
            .onChainHash(refreshed.getOrderHash() != null ? refreshed.getOrderHash() : localHash)
            .status(status)
            .isTampered(isTampered)
            .message(result.message())
            .txHash(refreshed.getBlockchainTxHash())
            .blockNumber(updatedBlockNumber)
            .build();
    }

    @Transactional
    public OrderResponse simulateTamper(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        order.setTotalAmount(BigDecimal.valueOf(1.00));
        Order saved = orderRepository.save(order);
        log.warn("DEMO: Simulated database tampering on Order #{}. TotalAmount changed to 1.00", orderId);
        return toResponse(saved);
    }

    @Transactional
    public OrderResponse restoreTamper(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        BigDecimal correctTotal = order.getItems().stream()
            .map(OrderItem::getLineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, java.math.RoundingMode.HALF_UP);
        order.setTotalAmount(correctTotal);
        Order saved = orderRepository.save(order);
        log.info("DEMO: Restored original database values for Order #{}. TotalAmount reset to {}", orderId, correctTotal);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public VerificationResponse verifyOrderByQuery(String query) {
        Order order;
        try {
            Long id = Long.parseLong(query.trim());
            order = orderRepository.findById(id)
                .or(() -> orderRepository.findByOrderNumber(query.trim()))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with query: " + query));
        } catch (NumberFormatException e) {
            order = orderRepository.findByOrderNumber(query.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with order number: " + query));
        }
        return verifyOrderOnChain(order.getId());
    }

    public OrderResponse toResponse(Order o) {
        List<OrderItemResponse> itemResponses = o.getItems().stream().map(oi ->
            OrderItemResponse.builder()
                .id(oi.getId())
                .productId(oi.getProduct() != null ? oi.getProduct().getId() : null)
                .productSku(oi.getProductSku())
                .productName(oi.getProductName())
                .quantity(oi.getQuantity())
                .unitPrice(oi.getUnitPrice())
                .discountPercent(oi.getDiscountPercent())
                .lineTotal(oi.getLineTotal())
                .build()
        ).collect(Collectors.toList());

        return OrderResponse.builder()
            .id(o.getId())
            .orderNumber(o.getOrderNumber())
            .customerId(o.getCustomer() != null ? o.getCustomer().getId() : null)
            .customerName(o.getCustomer() != null ? (o.getCustomer().getFirstName() + " " + o.getCustomer().getLastName()) : "N/A")
            .customerEmail(o.getCustomer() != null ? o.getCustomer().getEmail() : "N/A")
            .deliveryAddress(o.getDeliveryAddress())
            .status(o.getStatus())
            .subtotal(o.getSubtotal())
            .discountAmount(o.getDiscountAmount())
            .totalAmount(o.getTotalAmount())
            .notes(o.getNotes())
            .blockchainOrderId(o.getBlockchainOrderId())
            .blockchainTxHash(o.getBlockchainTxHash())
            .blockchainStatus(o.getBlockchainStatus())
            .orderHash(o.getOrderHash())
            .items(itemResponses)
            .createdAt(o.getCreatedAt())
            .updatedAt(o.getUpdatedAt())
            .build();
    }
}
