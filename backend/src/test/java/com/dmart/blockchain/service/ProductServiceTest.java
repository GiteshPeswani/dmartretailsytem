package com.dmart.blockchain.service;

import com.dmart.blockchain.blockchain.BlockchainService;
import com.dmart.blockchain.dto.request.ProductRequest;
import com.dmart.blockchain.dto.response.ProductResponse;
import com.dmart.blockchain.entity.Category;
import com.dmart.blockchain.entity.Product;
import com.dmart.blockchain.exception.BadRequestException;
import com.dmart.blockchain.repository.CategoryRepository;
import com.dmart.blockchain.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private BlockchainService blockchainService;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("Create product saves in DB and triggers async blockchain registration")
    void testCreateProductSuccess() {
        Category cat = Category.builder().id(1L).name("Grocery").slug("grocery").build();

        ProductRequest req = new ProductRequest();
        req.setSku("SKU-100");
        req.setName("Organic Rice");
        req.setCategoryId(1L);
        req.setPrice(BigDecimal.valueOf(150.00));
        req.setStockQuantity(50);

        when(productRepository.existsBySku("SKU-100")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));

        Product saved = Product.builder()
            .id(1L)
            .sku("SKU-100")
            .name("Organic Rice")
            .category(cat)
            .price(BigDecimal.valueOf(150.00))
            .stockQuantity(50)
            .build();
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        ProductResponse resp = productService.createProduct(req);

        assertThat(resp).isNotNull();
        assertThat(resp.getSku()).isEqualTo("SKU-100");
        verify(productRepository).save(any(Product.class));
        verify(blockchainService).registerProductAsync(saved);
    }

    @Test
    @DisplayName("Duplicate SKU throws BadRequestException")
    void testCreateProductDuplicateSku() {
        ProductRequest req = new ProductRequest();
        req.setSku("DUPLICATE-SKU");

        when(productRepository.existsBySku("DUPLICATE-SKU")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(req))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("SKU already exists");
    }
}
