package com.dmart.blockchain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private String productImageUrl;
    private BigDecimal unitPrice;
    private BigDecimal discountPercent;
    private BigDecimal effectivePrice;
    private int quantity;
    private BigDecimal lineTotal;
    private int availableStock;
}
