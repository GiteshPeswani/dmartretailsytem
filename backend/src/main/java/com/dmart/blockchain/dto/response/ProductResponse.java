package com.dmart.blockchain.dto.response;

import com.dmart.blockchain.entity.BlockchainStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String sku;
    private String name;
    private String description;
    private Long categoryId;
    private String categoryName;
    private BigDecimal price;
    private BigDecimal discountPercent;
    private BigDecimal effectivePrice;
    private int stockQuantity;
    private String imageUrl;
    private boolean active;
    private String blockchainProductId;
    private String blockchainTxHash;
    private BlockchainStatus blockchainStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
