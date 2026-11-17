package com.dmart.blockchain.service;

import com.dmart.blockchain.blockchain.BlockchainService;
import com.dmart.blockchain.blockchain.HashUtil;
import com.dmart.blockchain.dto.request.BatchCreateRequest;
import com.dmart.blockchain.dto.response.BatchResponse;
import com.dmart.blockchain.dto.response.VerificationResponse;
import com.dmart.blockchain.entity.*;
import com.dmart.blockchain.exception.BadRequestException;
import com.dmart.blockchain.exception.ResourceNotFoundException;
import com.dmart.blockchain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchService {

    private final ProductBatchRepository batchRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AuditEventRepository auditEventRepository;
    private final BlockchainService blockchainService;

    @Transactional
    public BatchResponse createBatch(BatchCreateRequest req, String actorEmail) {
        Product product = productRepository.findById(req.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + req.getProductId()));

        if (batchRepository.findByBatchNumber(req.getBatchNumber().trim()).isPresent()) {
            throw new BadRequestException("Batch number already exists: " + req.getBatchNumber());
        }

        User actor = userRepository.findByEmail(actorEmail).orElse(null);
        String actorRole = actor != null && !actor.getRoles().isEmpty()
            ? actor.getRoles().iterator().next().getName() : "ADMIN";

        byte[] dataHashBytes = HashUtil.hashBatch(
            req.getBatchNumber().trim(),
            product.getId(),
            req.getManufactureDate().toString(),
            req.getExpiryDate().toString()
        );

        ProductBatch batch = ProductBatch.builder()
            .batchNumber(req.getBatchNumber().trim())
            .product(product)
            .quantity(req.getQuantity())
            .manufactureDate(req.getManufactureDate())
            .expiryDate(req.getExpiryDate())
            .stage(ProductBatch.BatchStage.MANUFACTURED)
            .blockchainStatus(BlockchainStatus.PENDING)
            .dataHash(HashUtil.toHex(dataHashBytes))
            .build();

        ProductBatch saved = batchRepository.save(batch);

        auditEventRepository.save(AuditEvent.builder()
            .entityType(BlockchainTransaction.EntityType.BATCH)
            .entityId(saved.getId())
            .eventType("BATCH_MANUFACTURED")
            .actor(actor)
            .actorRole(actorRole)
            .description("Batch " + saved.getBatchNumber() + " registered for " + product.getName())
            .build());

        blockchainService.registerBatchAsync(saved);

        log.info("Batch created and queued for blockchain registration: {}", saved.getBatchNumber());
        return toResponse(saved);
    }

    @Transactional
    public BatchResponse recordWarehouseArrival(Long batchId, String actorEmail) {
        ProductBatch batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + batchId));

        if (batch.getStage() != ProductBatch.BatchStage.MANUFACTURED) {
            throw new BadRequestException("Batch has already passed warehouse arrival stage");
        }

        User actor = userRepository.findByEmail(actorEmail).orElse(null);
        String actorRole = actor != null && !actor.getRoles().isEmpty()
            ? actor.getRoles().iterator().next().getName() : "STORE_MANAGER";

        batch.setStage(ProductBatch.BatchStage.WAREHOUSE_ARRIVED);
        batch.setWarehouseReceivedAt(LocalDateTime.now());
        ProductBatch updated = batchRepository.save(batch);

        auditEventRepository.save(AuditEvent.builder()
            .entityType(BlockchainTransaction.EntityType.BATCH)
            .entityId(updated.getId())
            .eventType("BATCH_WAREHOUSE_ARRIVED")
            .actor(actor)
            .actorRole(actorRole)
            .description("Batch " + updated.getBatchNumber() + " received at distribution warehouse")
            .build());

        blockchainService.updateBatchStageOnChainAsync(updated, ProductBatch.BatchStage.WAREHOUSE_ARRIVED);

        log.info("Batch {} arrived at warehouse", updated.getBatchNumber());
        return toResponse(updated);
    }

    @Transactional
    public BatchResponse recordStoreArrival(Long batchId, String actorEmail) {
        ProductBatch batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + batchId));

        if (batch.getStage() != ProductBatch.BatchStage.WAREHOUSE_ARRIVED) {
            throw new BadRequestException("Batch must arrive at warehouse before store arrival");
        }

        User actor = userRepository.findByEmail(actorEmail).orElse(null);
        String actorRole = actor != null && !actor.getRoles().isEmpty()
            ? actor.getRoles().iterator().next().getName() : "STORE_MANAGER";

        batch.setStage(ProductBatch.BatchStage.STORE_ARRIVED);
        batch.setStoreReceivedAt(LocalDateTime.now());
        ProductBatch updated = batchRepository.save(batch);

        auditEventRepository.save(AuditEvent.builder()
            .entityType(BlockchainTransaction.EntityType.BATCH)
            .entityId(updated.getId())
            .eventType("BATCH_STORE_ARRIVED")
            .actor(actor)
            .actorRole(actorRole)
            .description("Batch " + updated.getBatchNumber() + " verified and received on store shelf")
            .build());

        blockchainService.updateBatchStageOnChainAsync(updated, ProductBatch.BatchStage.STORE_ARRIVED);

        log.info("Batch {} arrived at store shelf", updated.getBatchNumber());
        return toResponse(updated);
    }

    @Transactional(readOnly = true)
    public List<BatchResponse> getAllBatches() {
        return batchRepository.findAllWithProduct().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BatchResponse getBatchById(Long id) {
        ProductBatch b = batchRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + id));
        return toResponse(b);
    }

    @Transactional(readOnly = true)
    public BatchResponse getBatchByNumber(String batchNumber) {
        ProductBatch b = batchRepository.findByBatchNumber(batchNumber.trim())
            .orElseThrow(() -> new ResourceNotFoundException("Batch not found with batch number: " + batchNumber));
        return toResponse(b);
    }

    @Transactional(readOnly = true)
    public VerificationResponse verifyBatchOnChain(Long batchId) {
        ProductBatch batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new ResourceNotFoundException("Batch not found with id: " + batchId));

        byte[] localHashBytes = HashUtil.hashBatch(
            batch.getBatchNumber(),
            batch.getProduct().getId(),
            batch.getManufactureDate().toString(),
            batch.getExpiryDate().toString()
        );
        String localHash = HashUtil.toHex(localHashBytes);

        if (!blockchainService.isConnected()) {
            return VerificationResponse.builder()
                .verified(false)
                .blockchainAvailable(false)
                .entityType("BATCH")
                .entityId(batchId)
                .localHash(localHash)
                .onChainHash(null)
                .message("Blockchain node is currently offline or unreachable.")
                .txHash(batch.getBlockchainTxHash())
                .build();
        }

        BlockchainService.VerificationResult result = blockchainService.verifyBatchOnChain(batch);

        return VerificationResponse.builder()
            .verified(result.isVerified())
            .blockchainAvailable(result.isAvailable())
            .entityType("BATCH")
            .entityId(batchId)
            .localHash(localHash)
            .onChainHash(batch.getDataHash())
            .message(result.message())
            .txHash(batch.getBlockchainTxHash())
            .build();
    }

    public BatchResponse toResponse(ProductBatch b) {
        String qrPayload = "http://localhost:4200/batch-tracking?batchNumber=" + b.getBatchNumber();
        return BatchResponse.builder()
            .id(b.getId())
            .batchNumber(b.getBatchNumber())
            .productId(b.getProduct().getId())
            .productName(b.getProduct().getName())
            .productSku(b.getProduct().getSku())
            .quantity(b.getQuantity())
            .manufactureDate(b.getManufactureDate())
            .expiryDate(b.getExpiryDate())
            .stage(b.getStage().name())
            .warehouseReceivedAt(b.getWarehouseReceivedAt())
            .storeReceivedAt(b.getStoreReceivedAt())
            .blockchainBatchId(b.getBlockchainBatchId())
            .blockchainTxHash(b.getBlockchainTxHash())
            .blockchainStatus(b.getBlockchainStatus().name())
            .dataHash(b.getDataHash())
            .qrCodeData(qrPayload)
            .createdAt(b.getCreatedAt())
            .build();
    }
}
