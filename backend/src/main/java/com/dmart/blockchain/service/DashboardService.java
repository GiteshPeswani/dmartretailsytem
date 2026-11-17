package com.dmart.blockchain.service;

import com.dmart.blockchain.dto.response.DashboardSummaryResponse;
import com.dmart.blockchain.dto.response.OrderResponse;
import com.dmart.blockchain.entity.BlockchainStatus;
import com.dmart.blockchain.entity.OrderStatus;
import com.dmart.blockchain.repository.OrderRepository;
import com.dmart.blockchain.repository.ProductRepository;
import com.dmart.blockchain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary() {
        long totalProducts = productRepository.count();
        long totalCustomers = userRepository.countCustomers();
        long totalOrders = orderRepository.count();
        BigDecimal totalRevenue = orderRepository.sumTotalRevenue();
        long lowStockCount = productRepository.countLowStock();

        long confirmedOrders = orderRepository.findByBlockchainStatus(BlockchainStatus.CONFIRMED).size();
        long failedOrders = orderRepository.findByBlockchainStatus(BlockchainStatus.FAILED).size();
        long pendingOrders = orderRepository.findByBlockchainStatus(BlockchainStatus.PENDING).size();

        List<OrderResponse> recentOrders = orderRepository.findRecentOrders(PageRequest.of(0, 5))
            .stream()
            .map(orderService::toResponse)
            .collect(Collectors.toList());

        return DashboardSummaryResponse.builder()
            .totalProducts(totalProducts)
            .totalCustomers(totalCustomers)
            .totalOrders(totalOrders)
            .totalRevenue(totalRevenue != null ? totalRevenue : BigDecimal.ZERO)
            .lowStockCount(lowStockCount)
            .pendingOrdersCount(pendingOrders)
            .blockchainConfirmedOrders(confirmedOrders)
            .blockchainFailedOrders(failedOrders)
            .recentOrders(recentOrders)
            .build();
    }
}
