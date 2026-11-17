package com.dmart.blockchain.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BatchResponse {

    private Long id;
    private String batchNumber;
    private Long productId;
    private String productName;
    private String productSku;
    private int quantity;
    private LocalDate manufactureDate;
    private LocalDate expiryDate;
    private String stage;
    private LocalDateTime warehouseReceivedAt;
    private LocalDateTime storeReceivedAt;
    private String blockchainBatchId;
    private String blockchainTxHash;
    private String blockchainStatus;
    private String dataHash;
    private String qrCodeData;
    private LocalDateTime createdAt;
}
