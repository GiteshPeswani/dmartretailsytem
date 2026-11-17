package com.dmart.blockchain.controller;

import com.dmart.blockchain.dto.request.OrderCreateRequest;
import com.dmart.blockchain.dto.response.ApiResponse;
import com.dmart.blockchain.dto.response.OrderResponse;
import com.dmart.blockchain.dto.response.VerificationResponse;
import com.dmart.blockchain.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Tag(name = "Customer Orders", description = "Order checkout, history and blockchain verification")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    @Operation(summary = "Place order from current shopping cart")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody OrderCreateRequest req) {
        OrderResponse order = orderService.checkout(userDetails.getUsername(), req);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(order, "Order placed successfully! Submitting to blockchain..."));
    }

    @GetMapping
    @Operation(summary = "Get authenticated customer's order history")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<OrderResponse> orders = orderService.getCustomerOrders(userDetails.getUsername(), pageRequest);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get customer order details by ID")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        OrderResponse order = orderService.getCustomerOrderById(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/{id}/verify")
    @Operation(summary = "Verify customer order against blockchain registry")
    public ResponseEntity<ApiResponse<VerificationResponse>> verifyOrder(
            @PathVariable Long id) {
        VerificationResponse result = orderService.verifyOrderOnChain(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{id}/simulate-tamper")
    @Operation(summary = "Controlled tampering demo: alter MySQL order record to trigger TAMPERED detection")
    public ResponseEntity<ApiResponse<OrderResponse>> simulateTamper(@PathVariable Long id) {
        OrderResponse result = orderService.simulateTamper(id);
        return ResponseEntity.ok(ApiResponse.success(result, "MySQL order total altered to ₹1.00 for tampering demonstration."));
    }

    @PostMapping("/{id}/restore-tamper")
    @Operation(summary = "Controlled tampering demo: restore original MySQL order record to re-achieve VERIFIED")
    public ResponseEntity<ApiResponse<OrderResponse>> restoreTamper(@PathVariable Long id) {
        OrderResponse result = orderService.restoreTamper(id);
        return ResponseEntity.ok(ApiResponse.success(result, "Original MySQL order values restored successfully."));
    }

    @GetMapping("/verify-lookup")
    @Operation(summary = "Public order verification by entering Order ID or Order Number")
    public ResponseEntity<ApiResponse<VerificationResponse>> verifyByLookup(@RequestParam String query) {
        VerificationResponse result = orderService.verifyOrderByQuery(query);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
