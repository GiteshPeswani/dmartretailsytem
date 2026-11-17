package com.dmart.blockchain.service;

import com.dmart.blockchain.dto.request.CartItemRequest;
import com.dmart.blockchain.dto.response.CartItemResponse;
import com.dmart.blockchain.dto.response.CartSummaryResponse;
import com.dmart.blockchain.entity.CartItem;
import com.dmart.blockchain.entity.Product;
import com.dmart.blockchain.entity.User;
import com.dmart.blockchain.exception.BadRequestException;
import com.dmart.blockchain.exception.InsufficientStockException;
import com.dmart.blockchain.exception.ResourceNotFoundException;
import com.dmart.blockchain.repository.CartItemRepository;
import com.dmart.blockchain.repository.ProductRepository;
import com.dmart.blockchain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CartSummaryResponse getCart(String userEmail) {
        User user = getUser(userEmail);
        List<CartItem> items = cartItemRepository.findByUserIdWithProduct(user.getId());
        return buildCartSummary(items);
    }

    @Transactional
    public CartSummaryResponse addToCart(String userEmail, CartItemRequest req) {
        User user = getUser(userEmail);
        Product product = productRepository.findById(req.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + req.getProductId()));

        if (!product.isActive()) {
            throw new BadRequestException("Product is inactive and cannot be purchased: " + product.getName());
        }

        Optional<CartItem> existing = cartItemRepository.findByUserIdAndProductId(user.getId(), product.getId());
        int targetQty = req.getQuantity();

        if (existing.isPresent()) {
            targetQty += existing.get().getQuantity();
        }

        if (product.getStockQuantity() < targetQty) {
            throw new InsufficientStockException("Insufficient stock for " + product.getName() +
                ". Available: " + product.getStockQuantity() + ", Requested: " + targetQty);
        }

        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(targetQty);
            cartItemRepository.save(item);
        } else {
            CartItem item = CartItem.builder()
                .user(user)
                .product(product)
                .quantity(req.getQuantity())
                .build();
            cartItemRepository.save(item);
        }

        return getCart(userEmail);
    }

    @Transactional
    public CartSummaryResponse updateQuantity(String userEmail, Long productId, int quantity) {
        User user = getUser(userEmail);
        if (quantity <= 0) {
            cartItemRepository.findByUserIdAndProductId(user.getId(), productId)
                .ifPresent(cartItemRepository::delete);
            return getCart(userEmail);
        }

        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (product.getStockQuantity() < quantity) {
            throw new InsufficientStockException("Insufficient stock for " + product.getName() +
                ". Available: " + product.getStockQuantity() + ", Requested: " + quantity);
        }

        CartItem item = cartItemRepository.findByUserIdAndProductId(user.getId(), productId)
            .orElseThrow(() -> new ResourceNotFoundException("Item not found in cart"));

        item.setQuantity(quantity);
        cartItemRepository.save(item);

        return getCart(userEmail);
    }

    @Transactional
    public CartSummaryResponse removeFromCart(String userEmail, Long productId) {
        User user = getUser(userEmail);
        cartItemRepository.findByUserIdAndProductId(user.getId(), productId)
            .ifPresent(cartItemRepository::delete);
        return getCart(userEmail);
    }

    @Transactional
    public void clearCart(String userEmail) {
        User user = getUser(userEmail);
        cartItemRepository.deleteByUserId(user.getId());
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private CartSummaryResponse buildCartSummary(List<CartItem> items) {
        int totalCount = 0;
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal totalSavings = BigDecimal.ZERO;
        BigDecimal grandTotal = BigDecimal.ZERO;

        List<CartItemResponse> itemResponses = items.stream().map(item -> {
            Product p = item.getProduct();
            BigDecimal unitPrice = p.getPrice();
            BigDecimal effectivePrice = p.getEffectivePrice();
            BigDecimal lineTotal = effectivePrice.multiply(BigDecimal.valueOf(item.getQuantity()));
            BigDecimal lineSavings = unitPrice.subtract(effectivePrice).multiply(BigDecimal.valueOf(item.getQuantity()));

            return CartItemResponse.builder()
                .id(item.getId())
                .productId(p.getId())
                .productSku(p.getSku())
                .productName(p.getName())
                .productImageUrl(p.getImageUrl())
                .unitPrice(unitPrice)
                .discountPercent(p.getDiscountPercent())
                .effectivePrice(effectivePrice)
                .quantity(item.getQuantity())
                .lineTotal(lineTotal)
                .availableStock(p.getStockQuantity())
                .build();
        }).collect(Collectors.toList());

        for (CartItemResponse r : itemResponses) {
            totalCount += r.getQuantity();
            subtotal = subtotal.add(r.getUnitPrice().multiply(BigDecimal.valueOf(r.getQuantity())));
            grandTotal = grandTotal.add(r.getLineTotal());
        }
        totalSavings = subtotal.subtract(grandTotal);

        return CartSummaryResponse.builder()
            .items(itemResponses)
            .totalItemCount(totalCount)
            .subtotal(subtotal)
            .totalSavings(totalSavings)
            .grandTotal(grandTotal)
            .build();
    }
}
