package com.dmart.blockchain.blockchain;

import com.dmart.blockchain.entity.Order;
import com.dmart.blockchain.entity.OrderStatus;
import com.dmart.blockchain.entity.Product;
import org.web3j.crypto.Hash;

import java.math.RoundingMode;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Deterministic hashing utilities for on-chain record verification.
 *
 * Canonical product hash: keccak256(id | "|" | sku | "|" | name | "|" | price)
 * Canonical order hash:   keccak256(orderId | "|" | orderNumber | "|" | customerId | "|" | totalAmount | "|" | itemCount)
 */
public final class HashUtil {

    private HashUtil() {}

    public static byte[] hashProduct(Product p) {
        String canonical = p.getId() + "|" + p.getSku() + "|" + p.getName() + "|"
            + p.getPrice().setScale(2, RoundingMode.HALF_UP).toPlainString();
        return Hash.sha3(canonical.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] hashOrder(Order o) {
        String canonical = o.getId() + "|" + o.getOrderNumber() + "|"
            + o.getCustomer().getId() + "|"
            + o.getTotalAmount().setScale(2, RoundingMode.HALF_UP).toPlainString() + "|"
            + o.getItems().size();
        return Hash.sha3(canonical.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] hashStatusEvent(Order o, OrderStatus newStatus) {
        String canonical = o.getId() + "|" + newStatus.name() + "|" + System.currentTimeMillis();
        return Hash.sha3(canonical.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] hashBatch(String batchNumber, Long productId, String mfgDate, String expDate) {
        String canonical = batchNumber + "|" + productId + "|" + mfgDate + "|" + expDate;
        return Hash.sha3(canonical.getBytes(StandardCharsets.UTF_8));
    }

    public static byte[] hashBatchStageEvent(String batchNumber, String stage) {
        String canonical = batchNumber + "|" + stage + "|" + System.currentTimeMillis();
        return Hash.sha3(canonical.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Convert a string identifier to a right-zero-padded 32-byte array.
     */
    public static byte[] toBytes32(String value) {
        byte[] src = value.getBytes(StandardCharsets.UTF_8);
        byte[] dst = new byte[32];
        System.arraycopy(src, 0, dst, 0, Math.min(src.length, 32));
        return dst;
    }

    public static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder("0x");
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
