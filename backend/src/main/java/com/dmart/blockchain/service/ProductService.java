package com.dmart.blockchain.service;

import com.dmart.blockchain.blockchain.BlockchainService;
import com.dmart.blockchain.blockchain.HashUtil;
import com.dmart.blockchain.dto.request.ProductRequest;
import com.dmart.blockchain.dto.response.ProductResponse;
import com.dmart.blockchain.dto.response.VerificationResponse;
import com.dmart.blockchain.entity.BlockchainStatus;
import com.dmart.blockchain.entity.Category;
import com.dmart.blockchain.entity.Product;
import com.dmart.blockchain.exception.BadRequestException;
import com.dmart.blockchain.exception.ResourceNotFoundException;
import com.dmart.blockchain.repository.CategoryRepository;
import com.dmart.blockchain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BlockchainService blockchainService;

    @Transactional(readOnly = true)
    public Page<ProductResponse> getActiveProducts(Long categoryId, String search, Pageable pageable) {
        return productRepository.findActiveWithFilters(categoryId, search, pageable)
            .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProductsAdmin(Pageable pageable) {
        return productRepository.findAll(pageable)
            .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return toResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest req) {
        if (productRepository.existsBySku(req.getSku())) {
            throw new BadRequestException("SKU already exists: " + req.getSku());
        }

        Category category = categoryRepository.findById(req.getCategoryId())
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + req.getCategoryId()));

        Product product = Product.builder()
            .sku(req.getSku().trim().toUpperCase())
            .name(req.getName().trim())
            .description(req.getDescription())
            .category(category)
            .price(req.getPrice())
            .discountPercent(req.getDiscountPercent() != null ? req.getDiscountPercent() : java.math.BigDecimal.ZERO)
            .stockQuantity(req.getStockQuantity())
            .imageUrl(req.getImageUrl())
            .active(req.getActive() != null ? req.getActive() : true)
            .blockchainStatus(BlockchainStatus.NOT_REGISTERED)
            .build();

        Product saved = productRepository.save(product);
        log.info("Created product: {} (ID: {})", saved.getName(), saved.getId());

        // Asynchronously register on blockchain
        blockchainService.registerProductAsync(saved);

        return toResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest req) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (productRepository.existsBySkuAndIdNot(req.getSku(), id)) {
            throw new BadRequestException("SKU is already in use by another product: " + req.getSku());
        }

        if (req.getCategoryId() != null && !req.getCategoryId().equals(product.getCategory().getId())) {
            Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + req.getCategoryId()));
            product.setCategory(category);
        }

        product.setSku(req.getSku().trim().toUpperCase());
        product.setName(req.getName().trim());
        product.setDescription(req.getDescription());
        product.setPrice(req.getPrice());
        if (req.getDiscountPercent() != null) product.setDiscountPercent(req.getDiscountPercent());
        product.setStockQuantity(req.getStockQuantity());
        if (req.getImageUrl() != null) product.setImageUrl(req.getImageUrl());
        if (req.getActive() != null) product.setActive(req.getActive());

        Product updated = productRepository.save(product);
        return toResponse(updated);
    }

    @Transactional
    public void deactivateProduct(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        product.setActive(false);
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public VerificationResponse verifyProductOnChain(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        String localHash = HashUtil.toHex(HashUtil.hashProduct(product));

        if (!blockchainService.isConnected()) {
            return VerificationResponse.builder()
                .verified(false)
                .blockchainAvailable(false)
                .entityType("PRODUCT")
                .entityId(id)
                .localHash(localHash)
                .onChainHash(null)
                .message("Local blockchain is currently offline or disconnected.")
                .txHash(product.getBlockchainTxHash())
                .build();
        }

        BlockchainService.VerificationResult result = blockchainService.verifyProductOnChain(product);

        return VerificationResponse.builder()
            .verified(result.isVerified())
            .blockchainAvailable(result.isAvailable())
            .entityType("PRODUCT")
            .entityId(id)
            .localHash(localHash)
            .onChainHash(product.getBlockchainProductId() != null ? localHash : null)
            .message(result.message())
            .txHash(product.getBlockchainTxHash())
            .build();
    }

    public ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
            .id(p.getId())
            .sku(p.getSku())
            .name(p.getName())
            .description(p.getDescription())
            .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
            .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
            .price(p.getPrice())
            .discountPercent(p.getDiscountPercent())
            .effectivePrice(p.getEffectivePrice())
            .stockQuantity(p.getStockQuantity())
            .imageUrl(p.getImageUrl())
            .active(p.isActive())
            .blockchainProductId(p.getBlockchainProductId())
            .blockchainTxHash(p.getBlockchainTxHash())
            .blockchainStatus(p.getBlockchainStatus())
            .createdAt(p.getCreatedAt())
            .updatedAt(p.getUpdatedAt())
            .build();
    }
}
