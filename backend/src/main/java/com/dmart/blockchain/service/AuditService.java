package com.dmart.blockchain.service;

import com.dmart.blockchain.dto.response.AuditEventResponse;
import com.dmart.blockchain.dto.response.BlockchainTxResponse;
import com.dmart.blockchain.entity.AuditEvent;
import com.dmart.blockchain.entity.BlockchainTransaction;
import com.dmart.blockchain.entity.BlockchainTransaction.EntityType;
import com.dmart.blockchain.repository.AuditEventRepository;
import com.dmart.blockchain.repository.BlockchainTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditEventRepository auditEventRepository;
    private final BlockchainTransactionRepository txRepository;

    @Transactional(readOnly = true)
    public List<AuditEventResponse> getAuditTrail(EntityType entityType, Long entityId) {
        return auditEventRepository.findByEntityTypeAndEntityIdOrderByCreatedAtAsc(entityType, entityId)
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BlockchainTxResponse> getBlockchainTransactions(EntityType entityType, Long entityId) {
        return txRepository.findByEntityTypeAndEntityId(entityType, entityId)
            .stream()
            .map(this::toTxResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BlockchainTxResponse> getAllRecentTransactions() {
        return txRepository.findAll().stream()
            .sorted((a, b) -> b.getSubmittedAt().compareTo(a.getSubmittedAt()))
            .limit(50)
            .map(this::toTxResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public com.dmart.blockchain.dto.response.BlockchainStatsResponse getBlockchainStats() {
        return com.dmart.blockchain.dto.response.BlockchainStatsResponse.builder()
            .totalTransactions(txRepository.count())
            .successfulTransactions(txRepository.countByStatus(BlockchainTransaction.TxStatus.CONFIRMED))
            .pendingTransactions(txRepository.countByStatus(BlockchainTransaction.TxStatus.PENDING))
            .failedTransactions(txRepository.countByStatus(BlockchainTransaction.TxStatus.FAILED))
            .recentTransactions(getAllRecentTransactions())
            .build();
    }

    private AuditEventResponse toResponse(AuditEvent e) {
        return AuditEventResponse.builder()
            .id(e.getId())
            .entityType(e.getEntityType())
            .entityId(e.getEntityId())
            .eventType(e.getEventType())
            .actorId(e.getActor() != null ? e.getActor().getId() : null)
            .actorName(e.getActor() != null ? (e.getActor().getFirstName() + " " + e.getActor().getLastName()) : "System")
            .actorRole(e.getActorRole())
            .description(e.getDescription())
            .txHash(e.getBcTransaction() != null ? e.getBcTransaction().getTxHash() : null)
            .createdAt(e.getCreatedAt())
            .build();
    }

    private BlockchainTxResponse toTxResponse(BlockchainTransaction tx) {
        return BlockchainTxResponse.builder()
            .id(tx.getId())
            .txHash(tx.getTxHash())
            .entityType(tx.getEntityType())
            .entityId(tx.getEntityId())
            .eventType(tx.getEventType())
            .status(tx.getStatus())
            .blockNumber(tx.getBlockNumber())
            .gasUsed(tx.getGasUsed())
            .errorMessage(tx.getErrorMessage())
            .retryCount(tx.getRetryCount())
            .submittedAt(tx.getSubmittedAt())
            .confirmedAt(tx.getConfirmedAt())
            .build();
    }
}
