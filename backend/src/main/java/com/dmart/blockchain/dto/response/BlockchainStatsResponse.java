package com.dmart.blockchain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockchainStatsResponse {
    private long totalTransactions;
    private long successfulTransactions;
    private long pendingTransactions;
    private long failedTransactions;
    private List<BlockchainTxResponse> recentTransactions;
}
