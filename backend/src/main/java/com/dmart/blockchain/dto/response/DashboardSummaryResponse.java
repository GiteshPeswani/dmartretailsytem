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
public class DashboardSummaryResponse {
    private long totalProducts;
    private long totalCustomers;
    private long totalOrders;
    private BigDecimal totalRevenue;
    private long lowStockCount;
    private long pendingOrdersCount;
    private long blockchainConfirmedOrders;
    private long blockchainFailedOrders;
    private List<OrderResponse> recentOrders;
}
