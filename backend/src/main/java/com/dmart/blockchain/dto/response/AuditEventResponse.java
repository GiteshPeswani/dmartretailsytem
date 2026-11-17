package com.dmart.blockchain.dto.response;

import com.dmart.blockchain.entity.BlockchainTransaction.EntityType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventResponse {
    private Long id;
    private EntityType entityType;
    private Long entityId;
    private String eventType;
    private Long actorId;
    private String actorName;
    private String actorRole;
    private String description;
    private String txHash;
    private LocalDateTime createdAt;
}
