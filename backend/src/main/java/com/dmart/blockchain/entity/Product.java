package com.dmart.blockchain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private int stockQuantity = 0;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "blockchain_product_id", length = 66)
    private String blockchainProductId;

    @Column(name = "blockchain_tx_hash", length = 66)
    private String blockchainTxHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "blockchain_status", nullable = false, length = 20)
    @Builder.Default
    private BlockchainStatus blockchainStatus = BlockchainStatus.NOT_REGISTERED;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public BigDecimal getEffectivePrice() {
        if (discountPercent == null || discountPercent.compareTo(BigDecimal.ZERO) == 0) {
            return price.setScale(2, java.math.RoundingMode.HALF_UP);
        }
        BigDecimal discount = price.multiply(discountPercent)
            .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
        return price.subtract(discount).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
