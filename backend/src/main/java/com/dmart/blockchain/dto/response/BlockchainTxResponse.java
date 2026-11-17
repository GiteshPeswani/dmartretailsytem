package com.dmart.blockchain.dto.response;

import com.dmart.blockchain.entity.BlockchainTransaction.EntityType;
import com.dmart.blockchain.entity.BlockchainTransaction.TxStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockchainTxResponse {
    private Long id;
    private String txHash;
    private EntityType entityType;
    private Long entityId;
    private String eventType;
    private TxStatus status;
    private Long blockNumber;
    private Long gasUsed;
    private String errorMessage;
    private int retryCount;
    private LocalDateTime submittedAt;
    private LocalDateTime confirmedAt;
}
