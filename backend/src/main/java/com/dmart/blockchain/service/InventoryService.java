package com.dmart.blockchain.service;

import com.dmart.blockchain.dto.request.InventoryUpdateRequest;
import com.dmart.blockchain.dto.response.InventoryMovementResponse;
import com.dmart.blockchain.dto.response.ProductResponse;
import com.dmart.blockchain.entity.InventoryMovement;
import com.dmart.blockchain.entity.Product;
import com.dmart.blockchain.entity.User;
import com.dmart.blockchain.exception.BadRequestException;
import com.dmart.blockchain.exception.ResourceNotFoundException;
import com.dmart.blockchain.repository.InventoryMovementRepository;
import com.dmart.blockchain.repository.ProductRepository;
import com.dmart.blockchain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;
    private final InventoryMovementRepository movementRepository;
    private final UserRepository userRepository;
    private final ProductService productService;

    @Transactional
    public ProductResponse updateInventory(InventoryUpdateRequest req, String userEmail) {
        Product product = productRepository.findById(req.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + req.getProductId()));

        User user = userRepository.findByEmail(userEmail).orElse(null);

        int currentStock = product.getStockQuantity();
        int newStock = currentStock + req.getQuantityChange();

        if (newStock < 0) {
            throw new BadRequestException("Adjustment would cause negative inventory. Current: " +
                currentStock + ", Change: " + req.getQuantityChange());
        }

        product.setStockQuantity(newStock);
        Product saved = productRepository.save(product);

        InventoryMovement movement = InventoryMovement.builder()
            .product(saved)
            .user(user)
            .movementType(req.getMovementType())
            .quantityChange(req.getQuantityChange())
            .quantityBefore(currentStock)
            .quantityAfter(newStock)
            .reason(req.getReason())
            .build();
        movementRepository.save(movement);

        log.info("Inventory updated for product {}: {} -> {} (Change: {}) by {}",
            saved.getSku(), currentStock, newStock, req.getQuantityChange(), userEmail);

        return productService.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<InventoryMovementResponse> getProductMovements(Long productId, Pageable pageable) {
        return movementRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable)
            .map(this::toResponse);
    }

    private InventoryMovementResponse toResponse(InventoryMovement m) {
        return InventoryMovementResponse.builder()
            .id(m.getId())
            .productId(m.getProduct() != null ? m.getProduct().getId() : null)
            .productSku(m.getProduct() != null ? m.getProduct().getSku() : null)
            .productName(m.getProduct() != null ? m.getProduct().getName() : null)
            .userId(m.getUser() != null ? m.getUser().getId() : null)
            .userName(m.getUser() != null ? (m.getUser().getFirstName() + " " + m.getUser().getLastName()) : "System")
            .orderId(m.getOrder() != null ? m.getOrder().getId() : null)
            .movementType(m.getMovementType())
            .quantityChange(m.getQuantityChange())
            .quantityBefore(m.getQuantityBefore())
            .quantityAfter(m.getQuantityAfter())
            .reason(m.getReason())
            .createdAt(m.getCreatedAt())
            .build();
    }
}
