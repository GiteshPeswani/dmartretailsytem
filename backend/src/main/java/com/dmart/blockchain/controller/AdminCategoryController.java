package com.dmart.blockchain.controller;

import com.dmart.blockchain.dto.request.CategoryRequest;
import com.dmart.blockchain.dto.response.ApiResponse;
import com.dmart.blockchain.dto.response.CategoryResponse;
import com.dmart.blockchain.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Categories", description = "Category management for administrators")
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "List all categories for admin")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllCategoriesAdmin()));
    }

    @PostMapping
    @Operation(summary = "Create a new category")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CategoryRequest req) {
        CategoryResponse cat = categoryService.createCategory(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(cat, "Category created"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a category")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest req) {
        CategoryResponse cat = categoryService.updateCategory(id, req);
        return ResponseEntity.ok(ApiResponse.success(cat, "Category updated"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate a category")
    public ResponseEntity<ApiResponse<Void>> deactivateCategory(@PathVariable Long id) {
        categoryService.deactivateCategory(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Category deactivated"));
    }
}
