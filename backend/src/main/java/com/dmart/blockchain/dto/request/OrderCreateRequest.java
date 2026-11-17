package com.dmart.blockchain.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderCreateRequest {
    @NotBlank(message = "Delivery address is required")
    private String deliveryAddress;

    private String notes;
}
