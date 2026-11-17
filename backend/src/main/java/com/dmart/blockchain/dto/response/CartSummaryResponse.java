package com.dmart.blockchain.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartSummaryResponse {
    private List<CartItemResponse> items;
    private int totalItemCount;
    private BigDecimal subtotal;
    private BigDecimal totalSavings;
    private BigDecimal grandTotal;
}
