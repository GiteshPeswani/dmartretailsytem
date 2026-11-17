package com.dmart.blockchain.controller;

import com.dmart.blockchain.dto.request.OrderStatusUpdateRequest;
import com.dmart.blockchain.dto.response.ApiResponse;
import com.dmart.blockchain.dto.response.OrderResponse;
import com.dmart.blockchain.entity.OrderStatus;
import com.dmart.blockchain.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Orders", description = "Order management for administrators")
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "List all orders with status filter, search and pagination")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Page<OrderResponse> orders = orderService.getAllOrdersAdmin(
            status, search, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update order status (with state machine validation and blockchain audit)")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody OrderStatusUpdateRequest req) {
        OrderResponse updated = orderService.updateOrderStatus(id, req.getNewStatus(), req.getReason(), userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(updated, "Order status updated and audit logged"));
    }
}
