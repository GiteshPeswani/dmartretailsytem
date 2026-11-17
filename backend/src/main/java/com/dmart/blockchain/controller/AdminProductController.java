package com.dmart.blockchain.controller;

import com.dmart.blockchain.dto.request.ProductRequest;
import com.dmart.blockchain.dto.response.ApiResponse;
import com.dmart.blockchain.dto.response.ProductResponse;
import com.dmart.blockchain.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STORE_MANAGER')")
@Tag(name = "Admin Products", description = "Product catalog management for administrators")
public class AdminProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "List all products including inactive for admin")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        Page<ProductResponse> products = productService.getAllProductsAdmin(
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id")));
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @PostMapping
    @Operation(summary = "Create a new product and register on blockchain")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest req) {
        ProductResponse product = productService.createProduct(req);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(product, "Product created and blockchain registration queued"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing product")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest req) {
        ProductResponse product = productService.updateProduct(id, req);
        return ResponseEntity.ok(ApiResponse.success(product, "Product updated"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a product")
    public ResponseEntity<ApiResponse<Void>> deactivateProduct(@PathVariable Long id) {
        productService.deactivateProduct(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Product deactivated"));
    }
}
