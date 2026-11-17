package com.dmart.blockchain.blockchain;

import com.dmart.blockchain.entity.Order;
import com.dmart.blockchain.entity.OrderStatus;
import com.dmart.blockchain.entity.Product;
import com.dmart.blockchain.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class HashUtilTest {

    @Test
    @DisplayName("Deterministic product hashing produces identical hash for same product values")
    void testDeterministicProductHash() {
        Product p1 = Product.builder()
            .id(1L)
            .sku("SKU-001")
            .name("Basmati Rice")
            .price(BigDecimal.valueOf(299.00))
            .build();

        Product p2 = Product.builder()
            .id(1L)
            .sku("SKU-001")
            .name("Basmati Rice")
            .price(BigDecimal.valueOf(299.00))
            .build();

        byte[] hash1 = HashUtil.hashProduct(p1);
        byte[] hash2 = HashUtil.hashProduct(p2);

        assertThat(hash1).isEqualTo(hash2);
        assertThat(HashUtil.toHex(hash1)).isEqualTo(HashUtil.toHex(hash2));
    }

    @Test
    @DisplayName("Product hashing changes when price or name changes (tamper-evident)")
    void testTamperEvidentProductHash() {
        Product pOriginal = Product.builder()
            .id(1L)
            .sku("SKU-001")
            .name("Basmati Rice")
            .price(BigDecimal.valueOf(299.00))
            .build();

        Product pTampered = Product.builder()
            .id(1L)
            .sku("SKU-001")
            .name("Basmati Rice")
            .price(BigDecimal.valueOf(199.00)) // tampered price
            .build();

        byte[] hash1 = HashUtil.hashProduct(pOriginal);
        byte[] hash2 = HashUtil.hashProduct(pTampered);

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("Deterministic order hashing produces consistent hash for same order")
    void testDeterministicOrderHash() {
        User user = User.builder().id(5L).build();
        Order order = Order.builder()
            .id(10L)
            .orderNumber("DMART-100")
            .customer(user)
            .totalAmount(BigDecimal.valueOf(500.00))
            .items(new ArrayList<>())
            .build();

        byte[] hash1 = HashUtil.hashOrder(order);
        byte[] hash2 = HashUtil.hashOrder(order);

        assertThat(hash1).isEqualTo(hash2);
    }
}
