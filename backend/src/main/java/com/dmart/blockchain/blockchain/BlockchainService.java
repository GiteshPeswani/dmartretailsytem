package com.dmart.blockchain.blockchain;

import com.dmart.blockchain.entity.*;
import com.dmart.blockchain.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.DefaultGasProvider;

import jakarta.annotation.PostConstruct;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class BlockchainService {

    private final BlockchainTransactionRepository bcTxRepo;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ProductBatchRepository batchRepository;

    @Value("${app.blockchain.enabled:true}")
    private boolean blockchainEnabled;

    @Value("${app.blockchain.rpc-url:http://127.0.0.1:8545}")
    private String rpcUrl;

    @Value("${app.blockchain.contract-address:}")
    private String contractAddress;

    @Value("${app.blockchain.operator-private-key:}")
    private String operatorPrivateKey;

    @Value("${app.blockchain.retry-attempts:3}")
    private int maxRetryAttempts;

    private Web3j web3j;
    private Credentials credentials;
    private RetailAuditRegistryWrapper contract;
    private boolean connected = false;

    public BlockchainService(BlockchainTransactionRepository bcTxRepo,
                             ProductRepository productRepository,
                             OrderRepository orderRepository,
                             ProductBatchRepository batchRepository) {
        this.bcTxRepo = bcTxRepo;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.batchRepository = batchRepository;
    }

    @PostConstruct
    public void init() {
        if (!blockchainEnabled) {
            log.warn("Blockchain integration is DISABLED (app.blockchain.enabled=false)");
            return;
        }
        try {
            web3j = Web3j.build(new HttpService(rpcUrl));
            web3j.ethBlockNumber().send(); // connectivity check
            connected = true;
            log.info("Connected to blockchain node at {}", rpcUrl);

            if (!operatorPrivateKey.isBlank()) {
                credentials = Credentials.create(operatorPrivateKey);
                log.info("Operator wallet: {}", credentials.getAddress());
            }

            if (!contractAddress.isBlank() && credentials != null) {
                contract = RetailAuditRegistryWrapper.load(
                    contractAddress, web3j, credentials, new DefaultGasProvider());
                log.info("RetailAuditRegistry loaded at {}", contractAddress);
            } else {
                log.warn("Contract address or private key not configured — blockchain writes disabled");
            }
        } catch (Exception ex) {
            connected = false;
            log.warn("Blockchain node unavailable at startup ({}): {}", rpcUrl, ex.getMessage());
        }
    }

    public synchronized boolean isConnected() {
        if (!blockchainEnabled) return false;
        try {
            if (web3j == null) {
                web3j = Web3j.build(new HttpService(rpcUrl));
            }
            web3j.ethBlockNumber().send();
            connected = true;
            ensureContractLoaded();
            return true;
        } catch (Exception ex) {
            connected = false;
            return false;
        }
    }

    public synchronized boolean isContractReady() {
        return isConnected() && contract != null;
    }

    private synchronized void ensureContractLoaded() {
        try {
            if (credentials == null && !operatorPrivateKey.isBlank()) {
                credentials = Credentials.create(operatorPrivateKey);
            }
            if (credentials == null || web3j == null) return;

            boolean needsDeploy = true;
            if (!contractAddress.isBlank()) {
                var codeResp = web3j.ethGetCode(contractAddress, org.web3j.protocol.core.DefaultBlockParameterName.LATEST).send();
                String code = codeResp != null ? codeResp.getCode() : null;
                if (code != null && !code.equals("0x") && code.length() > 2) {
                    needsDeploy = false;
                }
            }

            if (needsDeploy) {
                log.warn("Smart contract not found at address '{}'. Auto-deploying RetailAuditRegistry to blockchain node...", contractAddress);
                contract = RetailAuditRegistryWrapper.deploy(web3j, credentials, new DefaultGasProvider()).send();
                contractAddress = contract.getContractAddress();
                log.info("RetailAuditRegistry successfully auto-deployed to: {}", contractAddress);
            } else if (contract == null) {
                contract = RetailAuditRegistryWrapper.load(
                    contractAddress, web3j, credentials, new DefaultGasProvider());
                log.info("RetailAuditRegistry loaded at: {}", contractAddress);
            }
        } catch (Exception ex) {
            log.error("Failed to load or auto-deploy smart contract: {}", ex.getMessage());
        }
    }

    // ── Product Registration ──────────────────────────────────────────────────

    @Async
    public void registerProductAsync(Product product) {
        if (!isContractReady()) {
            log.warn("Blockchain not ready — skipping product registration for {}", product.getId());
            return;
        }
        BlockchainTransaction tx = BlockchainTransaction.builder()
            .entityType(BlockchainTransaction.EntityType.PRODUCT)
            .entityId(product.getId())
            .eventType("PRODUCT_REGISTER")
            .status(BlockchainTransaction.TxStatus.PENDING)
            .submittedAt(LocalDateTime.now())
            .build();
        bcTxRepo.save(tx);
        executeProductRegistration(product, tx);
    }

    @Transactional
    public void executeProductRegistration(Product product, BlockchainTransaction tx) {
        try {
            Product currentProduct = productRepository.findById(product.getId()).orElse(product);
            byte[] productId = HashUtil.toBytes32(currentProduct.getId().toString());
            byte[] dataHash  = HashUtil.hashProduct(currentProduct);

            TransactionReceipt receipt = contract.registerProduct(productId, dataHash).send();

            tx.setTxHash(receipt.getTransactionHash());
            tx.setBlockNumber(receipt.getBlockNumber() != null ? receipt.getBlockNumber().longValue() : null);
            tx.setStatus(receipt.isStatusOK() ? BlockchainTransaction.TxStatus.CONFIRMED
                                               : BlockchainTransaction.TxStatus.FAILED);
            tx.setConfirmedAt(LocalDateTime.now());
            bcTxRepo.save(tx);

            if (receipt.isStatusOK()) {
                currentProduct.setBlockchainStatus(BlockchainStatus.CONFIRMED);
                currentProduct.setBlockchainTxHash(receipt.getTransactionHash());
                currentProduct.setBlockchainProductId("0x" + bytesToHex(productId));
                productRepository.save(currentProduct);
                log.info("Product {} registered on-chain. TxHash: {}", currentProduct.getId(), receipt.getTransactionHash());
            }
        } catch (Exception ex) {
            tx.setStatus(BlockchainTransaction.TxStatus.FAILED);
            tx.setErrorMessage(ex.getMessage());
            tx.setRetryCount(tx.getRetryCount() + 1);
            bcTxRepo.save(tx);
            productRepository.findById(product.getId()).ifPresent(p -> {
                p.setBlockchainStatus(BlockchainStatus.FAILED);
                productRepository.save(p);
            });
            log.error("Product blockchain registration failed for {}: {}", product.getId(), ex.getMessage());
        }
    }

    // ── Order Registration ────────────────────────────────────────────────────

    public void createOrderOnChainSync(Order order) {
        if (!isContractReady()) {
            ensureContractLoaded();
        }
        if (!isContractReady()) {
            log.warn("Blockchain not ready — skipping synchronous order registration for {}", order.getId());
            return;
        }
        BlockchainTransaction tx = BlockchainTransaction.builder()
            .entityType(BlockchainTransaction.EntityType.ORDER)
            .entityId(order.getId())
            .eventType("ORDER_CREATE")
            .status(BlockchainTransaction.TxStatus.PENDING)
            .submittedAt(LocalDateTime.now())
            .build();
        bcTxRepo.save(tx);
        executeOrderCreation(order, tx);
    }

    @Async
    public void createOrderOnChainAsync(Order order) {
        if (!isContractReady()) {
            log.warn("Blockchain not ready — skipping order registration for {}", order.getId());
            return;
        }
        BlockchainTransaction tx = BlockchainTransaction.builder()
            .entityType(BlockchainTransaction.EntityType.ORDER)
            .entityId(order.getId())
            .eventType("ORDER_CREATE")
            .status(BlockchainTransaction.TxStatus.PENDING)
            .submittedAt(LocalDateTime.now())
            .build();
        bcTxRepo.save(tx);
        executeOrderCreation(order, tx);
    }

    @Transactional
    public void executeOrderCreation(Order order, BlockchainTransaction tx) {
        try {
            Order currentOrder = orderRepository.findById(order.getId()).orElse(order);
            byte[] orderId   = HashUtil.toBytes32(currentOrder.getId().toString());
            byte[] orderHash = HashUtil.hashOrder(currentOrder);

            TransactionReceipt receipt = contract.createOrder(orderId, orderHash).send();

            tx.setTxHash(receipt.getTransactionHash());
            tx.setBlockNumber(receipt.getBlockNumber() != null ? receipt.getBlockNumber().longValue() : null);
            tx.setStatus(receipt.isStatusOK() ? BlockchainTransaction.TxStatus.CONFIRMED
                                               : BlockchainTransaction.TxStatus.FAILED);
            tx.setConfirmedAt(LocalDateTime.now());
            bcTxRepo.save(tx);

            if (receipt.isStatusOK()) {
                currentOrder.setBlockchainStatus(BlockchainStatus.CONFIRMED);
                currentOrder.setBlockchainTxHash(receipt.getTransactionHash());
                currentOrder.setBlockchainOrderId("0x" + bytesToHex(orderId));
                currentOrder.setOrderHash("0x" + bytesToHex(orderHash));
                orderRepository.save(currentOrder);
                log.info("Order {} created on-chain. TxHash: {}", currentOrder.getId(), receipt.getTransactionHash());
            }
        } catch (Exception ex) {
            tx.setStatus(BlockchainTransaction.TxStatus.FAILED);
            tx.setErrorMessage(ex.getMessage());
            tx.setRetryCount(tx.getRetryCount() + 1);
            bcTxRepo.save(tx);
            orderRepository.findById(order.getId()).ifPresent(o -> {
                o.setBlockchainStatus(BlockchainStatus.FAILED);
                orderRepository.save(o);
            });
            log.error("Order blockchain registration failed for {}: {}", order.getId(), ex.getMessage());
        }
    }

    // ── Order Status Update ───────────────────────────────────────────────────

    @Async
    public void updateOrderStatusOnChainAsync(Order order, OrderStatus newStatus) {
        if (!isContractReady()) {
            log.warn("Blockchain not ready — skipping status update for order {}", order.getId());
            return;
        }
        BlockchainTransaction tx = BlockchainTransaction.builder()
            .entityType(BlockchainTransaction.EntityType.ORDER)
            .entityId(order.getId())
            .eventType("ORDER_STATUS_" + newStatus.name())
            .status(BlockchainTransaction.TxStatus.PENDING)
            .submittedAt(LocalDateTime.now())
            .build();
        bcTxRepo.save(tx);

        try {
            byte[] orderId    = HashUtil.toBytes32(order.getId().toString());
            byte[] eventHash  = HashUtil.hashStatusEvent(order, newStatus);
            BigInteger status = BigInteger.valueOf(newStatus.ordinal());

            // Ensure order exists on the smart contract before updating status
            Boolean registered = contract.isOrderRegistered(orderId).send();
            if (Boolean.FALSE.equals(registered)) {
                byte[] orderHash = HashUtil.hashOrder(order);
                contract.createOrder(orderId, orderHash).send();
            }

            TransactionReceipt receipt = contract.updateOrderStatus(orderId, status, eventHash).send();

            tx.setTxHash(receipt.getTransactionHash());
            tx.setBlockNumber(receipt.getBlockNumber() != null ? receipt.getBlockNumber().longValue() : null);
            tx.setStatus(receipt.isStatusOK() ? BlockchainTransaction.TxStatus.CONFIRMED
                                               : BlockchainTransaction.TxStatus.FAILED);
            tx.setConfirmedAt(LocalDateTime.now());
            bcTxRepo.save(tx);

            if (receipt.isStatusOK()) {
                log.info("Order {} status → {} recorded on-chain. TxHash: {}",
                    order.getId(), newStatus, receipt.getTransactionHash());
            }
        } catch (Exception ex) {
            tx.setStatus(BlockchainTransaction.TxStatus.FAILED);
            tx.setErrorMessage(ex.getMessage());
            bcTxRepo.save(tx);
            log.error("Order status update blockchain failed for {}: {}", order.getId(), ex.getMessage());
        }
    }

    // ── Verification ──────────────────────────────────────────────────────────

    public VerificationResult verifyOrderOnChain(Order order) {
        if (!isContractReady()) {
            return VerificationResult.unavailable("Blockchain node not connected");
        }
        try {
            byte[] orderId   = HashUtil.toBytes32(order.getId().toString());
            byte[] orderHash = HashUtil.hashOrder(order);

            Boolean registered = contract.isOrderRegistered(orderId).send();
            if (Boolean.FALSE.equals(registered)) {
                log.info("Order #{} not found on current smart contract. Syncing order state...", order.getId());
                TransactionReceipt receipt = contract.createOrder(orderId, orderHash).send();
                order.setBlockchainStatus(BlockchainStatus.CONFIRMED);
                order.setBlockchainTxHash(receipt.getTransactionHash());
                order.setBlockchainOrderId("0x" + bytesToHex(orderId));
                order.setOrderHash("0x" + bytesToHex(orderHash));
                orderRepository.save(order);
                return VerificationResult.success();
            }

            Boolean matched  = contract.verifyOrder(orderId, orderHash).send();
            return Boolean.TRUE.equals(matched)
                ? VerificationResult.success()
                : VerificationResult.tampered("Record hash mismatch — data may have been altered");
        } catch (Exception ex) {
            return VerificationResult.unavailable("Verification error: " + ex.getMessage());
        }
    }

    public VerificationResult verifyProductOnChain(Product product) {
        if (!isContractReady()) {
            return VerificationResult.unavailable("Blockchain node not connected");
        }
        try {
            byte[] productId = HashUtil.toBytes32(product.getId().toString());
            byte[] dataHash  = HashUtil.hashProduct(product);
            Boolean matched  = contract.verifyProduct(productId, dataHash).send();
            return matched
                ? VerificationResult.success()
                : VerificationResult.tampered("Record hash mismatch");
        } catch (Exception ex) {
            return VerificationResult.unavailable("Verification error: " + ex.getMessage());
        }
    }

    // ── Batch Registration & Updates ──────────────────────────────────────────

    @Async
    public void registerBatchAsync(ProductBatch batch) {
        if (!isContractReady()) {
            log.warn("Blockchain not ready — skipping batch registration for {}", batch.getId());
            return;
        }
        BlockchainTransaction tx = BlockchainTransaction.builder()
            .entityType(BlockchainTransaction.EntityType.BATCH)
            .entityId(batch.getId())
            .eventType("BATCH_REGISTER")
            .status(BlockchainTransaction.TxStatus.PENDING)
            .submittedAt(LocalDateTime.now())
            .build();
        bcTxRepo.save(tx);
        executeBatchRegistration(batch, tx);
    }

    @Transactional
    public void executeBatchRegistration(ProductBatch batch, BlockchainTransaction tx) {
        try {
            ProductBatch currentBatch = batchRepository.findById(batch.getId()).orElse(batch);
            byte[] batchId = HashUtil.toBytes32(currentBatch.getBatchNumber());
            byte[] productId = HashUtil.toBytes32(currentBatch.getProduct().getId().toString());
            byte[] dataHash = HashUtil.hashBatch(
                currentBatch.getBatchNumber(),
                currentBatch.getProduct().getId(),
                currentBatch.getManufactureDate().toString(),
                currentBatch.getExpiryDate().toString()
            );

            TransactionReceipt receipt = contract.registerBatch(batchId, productId, dataHash).send();

            tx.setTxHash(receipt.getTransactionHash());
            tx.setBlockNumber(receipt.getBlockNumber() != null ? receipt.getBlockNumber().longValue() : null);
            tx.setStatus(receipt.isStatusOK() ? BlockchainTransaction.TxStatus.CONFIRMED
                                               : BlockchainTransaction.TxStatus.FAILED);
            tx.setConfirmedAt(LocalDateTime.now());
            bcTxRepo.save(tx);

            if (receipt.isStatusOK()) {
                currentBatch.setBlockchainStatus(BlockchainStatus.CONFIRMED);
                currentBatch.setBlockchainTxHash(receipt.getTransactionHash());
                currentBatch.setBlockchainBatchId("0x" + bytesToHex(batchId));
                currentBatch.setDataHash(HashUtil.toHex(dataHash));
                batchRepository.save(currentBatch);
                log.info("Batch {} registered on-chain. TxHash: {}", currentBatch.getBatchNumber(), receipt.getTransactionHash());
            }
        } catch (Exception ex) {
            tx.setStatus(BlockchainTransaction.TxStatus.FAILED);
            tx.setErrorMessage(ex.getMessage());
            tx.setRetryCount(tx.getRetryCount() + 1);
            bcTxRepo.save(tx);
            batchRepository.findById(batch.getId()).ifPresent(b -> {
                b.setBlockchainStatus(BlockchainStatus.FAILED);
                batchRepository.save(b);
            });
            log.error("Batch blockchain registration failed for {}: {}", batch.getId(), ex.getMessage());
        }
    }

    @Async
    public void updateBatchStageOnChainAsync(ProductBatch batch, ProductBatch.BatchStage newStage) {
        if (!isContractReady()) return;

        BlockchainTransaction tx = BlockchainTransaction.builder()
            .entityType(BlockchainTransaction.EntityType.BATCH)
            .entityId(batch.getId())
            .eventType("BATCH_STAGE_" + newStage.name())
            .status(BlockchainTransaction.TxStatus.PENDING)
            .submittedAt(LocalDateTime.now())
            .build();
        bcTxRepo.save(tx);

        try {
            byte[] batchId = HashUtil.toBytes32(batch.getBatchNumber());
            byte[] eventHash = HashUtil.hashBatchStageEvent(batch.getBatchNumber(), newStage.name());
            BigInteger stageVal = BigInteger.valueOf(newStage.ordinal());

            TransactionReceipt receipt = contract.updateBatchStage(batchId, stageVal, eventHash).send();

            tx.setTxHash(receipt.getTransactionHash());
            tx.setBlockNumber(receipt.getBlockNumber() != null ? receipt.getBlockNumber().longValue() : null);
            tx.setStatus(receipt.isStatusOK() ? BlockchainTransaction.TxStatus.CONFIRMED
                                               : BlockchainTransaction.TxStatus.FAILED);
            tx.setConfirmedAt(LocalDateTime.now());
            bcTxRepo.save(tx);
        } catch (Exception ex) {
            tx.setStatus(BlockchainTransaction.TxStatus.FAILED);
            tx.setErrorMessage(ex.getMessage());
            bcTxRepo.save(tx);
            log.error("Batch stage update blockchain failed for {}: {}", batch.getId(), ex.getMessage());
        }
    }

    public VerificationResult verifyBatchOnChain(ProductBatch batch) {
        if (!isContractReady()) {
            return VerificationResult.unavailable("Blockchain node not connected");
        }
        try {
            byte[] batchId = HashUtil.toBytes32(batch.getBatchNumber());
            byte[] dataHash = HashUtil.hashBatch(
                batch.getBatchNumber(),
                batch.getProduct().getId(),
                batch.getManufactureDate().toString(),
                batch.getExpiryDate().toString()
            );
            Boolean matched = contract.verifyBatch(batchId, dataHash).send();
            return matched
                ? VerificationResult.success()
                : VerificationResult.tampered("Batch record hash mismatch — data may have been altered");
        } catch (Exception ex) {
            return VerificationResult.unavailable("Verification error: " + ex.getMessage());
        }
    }

    // ── Retry Scheduler ──────────────────────────────────────────────────────

    @Scheduled(fixedDelayString = "${app.blockchain.retry-delay-ms:30000}")
    @Transactional
    public void retryFailedTransactions() {
        if (!isContractReady()) return;

        List<BlockchainTransaction> failed = bcTxRepo
            .findByStatusAndRetryCountLessThan(BlockchainTransaction.TxStatus.FAILED, maxRetryAttempts);

        for (BlockchainTransaction tx : failed) {
            log.info("Retrying blockchain tx for {} entity {} event {}", tx.getEntityType(), tx.getEntityId(), tx.getEventType());
            if (tx.getEntityType() == BlockchainTransaction.EntityType.PRODUCT && "PRODUCT_REGISTER".equals(tx.getEventType())) {
                productRepository.findById(tx.getEntityId())
                    .ifPresent(p -> executeProductRegistration(p, tx));
            } else if (tx.getEntityType() == BlockchainTransaction.EntityType.ORDER && "ORDER_CREATE".equals(tx.getEventType())) {
                orderRepository.findById(tx.getEntityId())
                    .ifPresent(o -> executeOrderCreation(o, tx));
            } else if (tx.getEntityType() == BlockchainTransaction.EntityType.BATCH && "BATCH_REGISTER".equals(tx.getEventType())) {
                batchRepository.findById(tx.getEntityId())
                    .ifPresent(b -> executeBatchRegistration(b, tx));
            } else {
                // If it's a status transition that already failed, mark retry count to max so it stops looping
                tx.setRetryCount(maxRetryAttempts);
                bcTxRepo.save(tx);
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    public record VerificationResult(boolean isVerified, boolean isAvailable, String message) {
        public static VerificationResult success() {
            return new VerificationResult(true, true, "Record integrity verified");
        }
        public static VerificationResult tampered(String msg) {
            return new VerificationResult(false, true, msg);
        }
        public static VerificationResult unavailable(String msg) {
            return new VerificationResult(false, false, msg);
        }
    }
}
