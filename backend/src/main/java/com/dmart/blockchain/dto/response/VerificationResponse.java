package com.dmart.blockchain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResponse {
    private boolean verified;
    private boolean blockchainAvailable;
    private String entityType;
    private Long entityId;
    private String localHash;
    private String onChainHash;
    private String message;
    private String txHash;
    private Long blockNumber;
    private String status; // "VERIFIED" or "TAMPERED"
    private boolean isTampered;
}
