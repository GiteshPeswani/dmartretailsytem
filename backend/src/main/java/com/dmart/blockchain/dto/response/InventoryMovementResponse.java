package com.dmart.blockchain.dto.response;

import com.dmart.blockchain.entity.InventoryMovement.MovementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryMovementResponse {
    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private Long userId;
    private String userName;
    private Long orderId;
    private MovementType movementType;
    private int quantityChange;
    private int quantityBefore;
    private int quantityAfter;
    private String reason;
    private LocalDateTime createdAt;
}
