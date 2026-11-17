package com.dmart.blockchain.controller;

import com.dmart.blockchain.dto.request.BatchCreateRequest;
import com.dmart.blockchain.dto.response.ApiResponse;
import com.dmart.blockchain.dto.response.BatchResponse;
import com.dmart.blockchain.dto.response.VerificationResponse;
import com.dmart.blockchain.service.BatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/batches")
@RequiredArgsConstructor
@Tag(name = "Product Batches", description = "Product batch supply chain tracking and blockchain verification")
public class BatchController {

    private final BatchService batchService;

    @GetMapping
    @Operation(summary = "List all product batches")
    public ResponseEntity<ApiResponse<List<BatchResponse>>> getAllBatches() {
        return ResponseEntity.ok(ApiResponse.success(batchService.getAllBatches()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get batch by database ID")
    public ResponseEntity<ApiResponse<BatchResponse>> getBatchById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(batchService.getBatchById(id)));
    }

    @GetMapping("/by-number/{batchNumber}")
    @Operation(summary = "Public lookup of product batch by Batch ID / QR Code")
    public ResponseEntity<ApiResponse<BatchResponse>> getBatchByNumber(@PathVariable String batchNumber) {
        return ResponseEntity.ok(ApiResponse.success(batchService.getBatchByNumber(batchNumber)));
    }

    @GetMapping("/{id}/verify")
    @Operation(summary = "Verify product batch provenance on blockchain")
    public ResponseEntity<ApiResponse<VerificationResponse>> verifyBatch(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(batchService.verifyBatchOnChain(id)));
    }

    @PostMapping
    @Operation(summary = "Create product batch (Store Manager / Admin)")
    public ResponseEntity<ApiResponse<BatchResponse>> createBatch(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BatchCreateRequest req) {
        String actor = userDetails != null ? userDetails.getUsername() : "SYSTEM";
        BatchResponse batch = batchService.createBatch(req, actor);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(batch, "Product batch registered and queued for blockchain confirmation."));
    }

    @PutMapping("/{id}/warehouse-arrival")
    @Operation(summary = "Record warehouse arrival event on blockchain")
    public ResponseEntity<ApiResponse<BatchResponse>> recordWarehouseArrival(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        String actor = userDetails != null ? userDetails.getUsername() : "STORE_MANAGER";
        BatchResponse batch = batchService.recordWarehouseArrival(id, actor);
        return ResponseEntity.ok(ApiResponse.success(batch, "Warehouse arrival recorded on blockchain."));
    }

    @PutMapping("/{id}/store-arrival")
    @Operation(summary = "Record store arrival event on blockchain")
    public ResponseEntity<ApiResponse<BatchResponse>> recordStoreArrival(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        String actor = userDetails != null ? userDetails.getUsername() : "STORE_MANAGER";
        BatchResponse batch = batchService.recordStoreArrival(id, actor);
        return ResponseEntity.ok(ApiResponse.success(batch, "Store shelf arrival recorded on blockchain."));
    }
}
