package com.dmart.blockchain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_batches")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductBatch {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_number", nullable = false, unique = true, length = 100)
    private String batchNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    @Builder.Default
    private int quantity = 100;

    @Column(name = "manufacture_date", nullable = false)
    private LocalDate manufactureDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private BatchStage stage = BatchStage.MANUFACTURED;

    @Column(name = "warehouse_received_at")
    private LocalDateTime warehouseReceivedAt;

    @Column(name = "store_received_at")
    private LocalDateTime storeReceivedAt;

    @Column(name = "blockchain_batch_id", length = 66)
    private String blockchainBatchId;

    @Column(name = "blockchain_tx_hash", length = 66)
    private String blockchainTxHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "blockchain_status", nullable = false, length = 20)
    @Builder.Default
    private BlockchainStatus blockchainStatus = BlockchainStatus.NOT_REGISTERED;

    @Column(name = "data_hash", length = 66)
    private String dataHash;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum BatchStage {
        MANUFACTURED,
        WAREHOUSE_ARRIVED,
        STORE_ARRIVED
    }
}
