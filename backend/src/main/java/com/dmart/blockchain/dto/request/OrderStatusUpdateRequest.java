package com.dmart.blockchain.dto.request;

import com.dmart.blockchain.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderStatusUpdateRequest {
    @NotNull(message = "New status is required")
    private OrderStatus newStatus;

    private String reason;
}
