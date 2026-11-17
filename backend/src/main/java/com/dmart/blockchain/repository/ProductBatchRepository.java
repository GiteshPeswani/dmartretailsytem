package com.dmart.blockchain.repository;

import com.dmart.blockchain.entity.ProductBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductBatchRepository extends JpaRepository<ProductBatch, Long> {

    Optional<ProductBatch> findByBatchNumber(String batchNumber);

    List<ProductBatch> findByProductId(Long productId);

    @Query("SELECT pb FROM ProductBatch pb JOIN FETCH pb.product ORDER BY pb.id DESC")
    List<ProductBatch> findAllWithProduct();
}
