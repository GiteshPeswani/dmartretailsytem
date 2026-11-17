package com.dmart.blockchain.controller;

import com.dmart.blockchain.blockchain.BlockchainService;
import com.dmart.blockchain.dto.response.ApiResponse;
import com.dmart.blockchain.dto.response.AuditEventResponse;
import com.dmart.blockchain.dto.response.BlockchainTxResponse;
import com.dmart.blockchain.entity.BlockchainTransaction.EntityType;
import com.dmart.blockchain.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/blockchain")
@RequiredArgsConstructor
@Tag(name = "Blockchain Explorer", description = "Query blockchain audit trails, transactions and node status")
public class BlockchainController {

    private final BlockchainService blockchainService;
    private final AuditService auditService;

    @GetMapping("/status")
    @Operation(summary = "Get blockchain node connectivity and contract readiness status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
            "connected", blockchainService.isConnected(),
            "contractReady", blockchainService.isContractReady()
        )));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get transaction metrics and recent ledger items for admin dashboard")
    public ResponseEntity<ApiResponse<com.dmart.blockchain.dto.response.BlockchainStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(auditService.getBlockchainStats()));
    }

    @GetMapping("/transactions")
    @Operation(summary = "List recent blockchain transactions submitted by the system")
    public ResponseEntity<ApiResponse<List<BlockchainTxResponse>>> getRecentTransactions() {
        return ResponseEntity.ok(ApiResponse.success(auditService.getAllRecentTransactions()));
    }

    @GetMapping("/audit/{entityType}/{entityId}")
    @Operation(summary = "Get immutable audit trail for a product or order")
    public ResponseEntity<ApiResponse<List<AuditEventResponse>>> getAuditTrail(
            @PathVariable EntityType entityType,
            @PathVariable Long entityId) {
        return ResponseEntity.ok(ApiResponse.success(auditService.getAuditTrail(entityType, entityId)));
    }

    @GetMapping("/transactions/{entityType}/{entityId}")
    @Operation(summary = "Get blockchain transactions for a specific product or order")
    public ResponseEntity<ApiResponse<List<BlockchainTxResponse>>> getEntityTransactions(
            @PathVariable EntityType entityType,
            @PathVariable Long entityId) {
        return ResponseEntity.ok(ApiResponse.success(auditService.getBlockchainTransactions(entityType, entityId)));
    }
}
