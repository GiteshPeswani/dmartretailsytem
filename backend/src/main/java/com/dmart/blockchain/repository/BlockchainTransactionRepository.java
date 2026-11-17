package com.dmart.blockchain.repository;

import com.dmart.blockchain.entity.BlockchainTransaction;
import com.dmart.blockchain.entity.BlockchainTransaction.EntityType;
import com.dmart.blockchain.entity.BlockchainTransaction.TxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockchainTransactionRepository extends JpaRepository<BlockchainTransaction, Long> {
    List<BlockchainTransaction> findByEntityTypeAndEntityId(EntityType entityType, Long entityId);
    List<BlockchainTransaction> findByStatus(TxStatus status);
    Optional<BlockchainTransaction> findByTxHash(String txHash);
    List<BlockchainTransaction> findByStatusAndRetryCountLessThan(TxStatus status, int maxRetries);
    Optional<BlockchainTransaction> findTopByEntityTypeAndEntityIdOrderBySubmittedAtDesc(EntityType entityType, Long entityId);
    long countByStatus(TxStatus status);
}
