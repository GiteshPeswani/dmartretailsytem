package com.dmart.blockchain.blockchain;

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Minimal Web3j wrapper for RetailAuditRegistry smart contract.
 * Provides typed Java methods for each contract function.
 */
public class RetailAuditRegistryWrapper extends Contract {

    public static String getBinary() {
        try (var is = RetailAuditRegistryWrapper.class.getResourceAsStream("/blockchain/RetailAuditRegistry.json")) {
            if (is != null) {
                String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                int idx = json.indexOf("\"bytecode\":");
                if (idx != -1) {
                    int start = json.indexOf("\"", idx + 11) + 1;
                    int end = json.indexOf("\"", start);
                    return json.substring(start, end);
                }
            }
        } catch (Exception ignored) {}
        return "";
    }

    protected RetailAuditRegistryWrapper(String contractAddress, Web3j web3j,
                                          Credentials credentials, ContractGasProvider gasProvider) {
        super(getBinary(), contractAddress, web3j, credentials, gasProvider);
    }

    public static RetailAuditRegistryWrapper load(String contractAddress, Web3j web3j,
                                                   Credentials credentials, ContractGasProvider gasProvider) {
        return new RetailAuditRegistryWrapper(contractAddress, web3j, credentials, gasProvider);
    }

    public static RemoteCall<RetailAuditRegistryWrapper> deploy(
            Web3j web3j, Credentials credentials, ContractGasProvider gasProvider) {
        return deployRemoteCall(
            RetailAuditRegistryWrapper.class, web3j, credentials, gasProvider, getBinary(), "");
    }

    // ── registerProduct(bytes32 productId, bytes32 dataHash) ─────────────────
    public RemoteFunctionCall<TransactionReceipt> registerProduct(byte[] productId, byte[] dataHash) {
        Function function = new Function(
            "registerProduct",
            Arrays.asList(new Bytes32(productId), new Bytes32(dataHash)),
            Collections.emptyList()
        );
        return executeRemoteCallTransaction(function);
    }

    // ── createOrder(bytes32 orderId, bytes32 orderHash) ──────────────────────
    public RemoteFunctionCall<TransactionReceipt> createOrder(byte[] orderId, byte[] orderHash) {
        Function function = new Function(
            "createOrder",
            Arrays.asList(new Bytes32(orderId), new Bytes32(orderHash)),
            Collections.emptyList()
        );
        return executeRemoteCallTransaction(function);
    }

    // ── updateOrderStatus(bytes32 orderId, uint8 newStatus, bytes32 eventHash) ─
    public RemoteFunctionCall<TransactionReceipt> updateOrderStatus(byte[] orderId, BigInteger newStatus, byte[] eventHash) {
        Function function = new Function(
            "updateOrderStatus",
            Arrays.asList(new Bytes32(orderId), new Uint8(newStatus), new Bytes32(eventHash)),
            Collections.emptyList()
        );
        return executeRemoteCallTransaction(function);
    }

    // ── isOrderRegistered(bytes32 orderId) returns (bool) ────────────────────
    public RemoteFunctionCall<Boolean> isOrderRegistered(byte[] orderId) {
        Function function = new Function(
            "isOrderRegistered",
            List.of(new Bytes32(orderId)),
            List.of(new TypeReference<Bool>() {})
        );
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    // ── verifyOrder(bytes32 orderId, bytes32 orderHash) returns (bool) ────────
    public RemoteFunctionCall<Boolean> verifyOrder(byte[] orderId, byte[] orderHash) {
        Function function = new Function(
            "verifyOrder",
            Arrays.asList(new Bytes32(orderId), new Bytes32(orderHash)),
            List.of(new TypeReference<Bool>() {})
        );
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    // ── verifyProduct(bytes32 productId, bytes32 dataHash) returns (bool) ────
    public RemoteFunctionCall<Boolean> verifyProduct(byte[] productId, byte[] dataHash) {
        Function function = new Function(
            "verifyProduct",
            Arrays.asList(new Bytes32(productId), new Bytes32(dataHash)),
            List.of(new TypeReference<Bool>() {})
        );
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    // ── registerBatch(bytes32 batchId, bytes32 productId, bytes32 dataHash) ──
    public RemoteFunctionCall<TransactionReceipt> registerBatch(byte[] batchId, byte[] productId, byte[] dataHash) {
        Function function = new Function(
            "registerBatch",
            Arrays.asList(new Bytes32(batchId), new Bytes32(productId), new Bytes32(dataHash)),
            Collections.emptyList()
        );
        return executeRemoteCallTransaction(function);
    }

    // ── updateBatchStage(bytes32 batchId, uint8 newStage, bytes32 eventHash) ──
    public RemoteFunctionCall<TransactionReceipt> updateBatchStage(byte[] batchId, BigInteger newStage, byte[] eventHash) {
        Function function = new Function(
            "updateBatchStage",
            Arrays.asList(new Bytes32(batchId), new Uint8(newStage), new Bytes32(eventHash)),
            Collections.emptyList()
        );
        return executeRemoteCallTransaction(function);
    }

    // ── verifyBatch(bytes32 batchId, bytes32 dataHash) returns (bool) ────────
    public RemoteFunctionCall<Boolean> verifyBatch(byte[] batchId, byte[] dataHash) {
        Function function = new Function(
            "verifyBatch",
            Arrays.asList(new Bytes32(batchId), new Bytes32(dataHash)),
            List.of(new TypeReference<Bool>() {})
        );
        return executeRemoteCallSingleValueReturn(function, Boolean.class);
    }

    // ── getOrderAuditTrailLength(bytes32 orderId) returns (uint256) ──────────
    public RemoteFunctionCall<BigInteger> getOrderAuditTrailLength(byte[] orderId) {
        Function function = new Function(
            "getOrderAuditTrailLength",
            List.of(new Bytes32(orderId)),
            List.of(new TypeReference<org.web3j.abi.datatypes.generated.Uint256>() {})
        );
        return executeRemoteCallSingleValueReturn(function, BigInteger.class);
    }
}
