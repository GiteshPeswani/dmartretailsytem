package com.dmart.blockchain.controller;

import com.dmart.blockchain.dto.response.ApiResponse;
import com.dmart.blockchain.dto.response.ProductResponse;
import com.dmart.blockchain.dto.response.VerificationResponse;
import com.dmart.blockchain.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Public product browsing and verification")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Browse active products with category filtering, search, and pagination")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProducts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "id,desc") String sort) {

        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(direction, sortParts[0]));

        Page<ProductResponse> products = productService.getActiveProducts(categoryId, search, pageRequest);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product details by ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/{id}/verify")
    @Operation(summary = "Verify product integrity against local blockchain registry")
    public ResponseEntity<ApiResponse<VerificationResponse>> verifyProduct(@PathVariable Long id) {
        VerificationResponse result = productService.verifyProductOnChain(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
