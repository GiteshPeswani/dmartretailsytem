package com.dmart.blockchain.controller;

import com.dmart.blockchain.dto.request.CartItemRequest;
import com.dmart.blockchain.dto.response.ApiResponse;
import com.dmart.blockchain.dto.response.CartSummaryResponse;
import com.dmart.blockchain.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping cart operations for authenticated users")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get current user's shopping cart")
    public ResponseEntity<ApiResponse<CartSummaryResponse>> getCart(@AuthenticationPrincipal UserDetails userDetails) {
        CartSummaryResponse cart = cartService.getCart(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to shopping cart")
    public ResponseEntity<ApiResponse<CartSummaryResponse>> addToCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CartItemRequest req) {
        CartSummaryResponse cart = cartService.addToCart(userDetails.getUsername(), req);
        return ResponseEntity.ok(ApiResponse.success(cart, "Item added to cart"));
    }

    @PutMapping("/items/{productId}")
    @Operation(summary = "Update item quantity in cart")
    public ResponseEntity<ApiResponse<CartSummaryResponse>> updateQuantity(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId,
            @RequestParam int quantity) {
        CartSummaryResponse cart = cartService.updateQuantity(userDetails.getUsername(), productId, quantity);
        return ResponseEntity.ok(ApiResponse.success(cart, "Cart updated"));
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove product from cart")
    public ResponseEntity<ApiResponse<CartSummaryResponse>> removeItem(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId) {
        CartSummaryResponse cart = cartService.removeFromCart(userDetails.getUsername(), productId);
        return ResponseEntity.ok(ApiResponse.success(cart, "Item removed from cart"));
    }

    @DeleteMapping
    @Operation(summary = "Clear shopping cart")
    public ResponseEntity<ApiResponse<Void>> clearCart(@AuthenticationPrincipal UserDetails userDetails) {
        cartService.clearCart(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "Cart cleared"));
    }
}
