package com.dmart.blockchain.repository;

import com.dmart.blockchain.entity.Product;
import com.dmart.blockchain.entity.BlockchainStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySku(String sku);
    boolean existsBySkuAndIdNot(String sku, Long id);
    Optional<Product> findBySku(String sku);

    Page<Product> findByActiveTrue(Pageable pageable);

    @Query("SELECT p FROM Product p JOIN FETCH p.category c " +
           "WHERE p.active = true AND (:categoryId IS NULL OR c.id = :categoryId) " +
           "AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> findActiveWithFilters(
        @Param("categoryId") Long categoryId,
        @Param("search") String search,
        Pageable pageable);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.active = true AND p.stockQuantity < 10")
    long countLowStock();

    List<Product> findByBlockchainStatus(BlockchainStatus status);

    @Query("SELECT SUM(p.stockQuantity) FROM Product p WHERE p.active = true")
    Long sumTotalStock();
}
