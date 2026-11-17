package com.dmart.blockchain.controller;

import com.dmart.blockchain.dto.request.InventoryUpdateRequest;
import com.dmart.blockchain.dto.request.OrderStatusUpdateRequest;
import com.dmart.blockchain.dto.response.ApiResponse;
import com.dmart.blockchain.dto.response.InventoryMovementResponse;
import com.dmart.blockchain.dto.response.OrderResponse;
import com.dmart.blockchain.dto.response.ProductResponse;
import com.dmart.blockchain.entity.OrderStatus;
import com.dmart.blockchain.service.InventoryService;
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
@RequestMapping("/manager")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STORE_MANAGER', 'ADMIN')")
@Tag(name = "Store Manager Operations", description = "Inventory adjustments, stock movements and order preparation")
public class StoreManagerController {

    private final InventoryService inventoryService;
    private final OrderService orderService;

    @PostMapping("/inventory/adjust")
    @Operation(summary = "Adjust product inventory with reason code")
    public ResponseEntity<ApiResponse<ProductResponse>> adjustInventory(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody InventoryUpdateRequest req) {
        ProductResponse product = inventoryService.updateInventory(req, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(product, "Inventory adjusted successfully"));
    }

    @GetMapping("/inventory/{productId}/movements")
    @Operation(summary = "Get inventory movement history for a product")
    public ResponseEntity<ApiResponse<Page<InventoryMovementResponse>>> getProductMovements(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Page<InventoryMovementResponse> movements = inventoryService.getProductMovements(
            productId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(movements));
    }

    @GetMapping("/orders")
    @Operation(summary = "List permitted orders for store operations")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Page<OrderResponse> orders = orderService.getAllOrdersAdmin(
            status, search, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PatchMapping("/orders/{id}/status")
    @Operation(summary = "Update order status for packing, dispatch and delivery")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody OrderStatusUpdateRequest req) {
        OrderResponse updated = orderService.updateOrderStatus(id, req.getNewStatus(), req.getReason(), userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(updated, "Order status updated and audit logged"));
    }
}
