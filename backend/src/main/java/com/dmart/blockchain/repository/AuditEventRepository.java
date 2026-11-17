package com.dmart.blockchain.repository;

import com.dmart.blockchain.entity.AuditEvent;
import com.dmart.blockchain.entity.BlockchainTransaction.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByEntityTypeAndEntityIdOrderByCreatedAtAsc(EntityType entityType, Long entityId);
}
