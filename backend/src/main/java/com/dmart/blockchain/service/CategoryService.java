package com.dmart.blockchain.service;

import com.dmart.blockchain.dto.request.CategoryRequest;
import com.dmart.blockchain.dto.response.CategoryResponse;
import com.dmart.blockchain.entity.Category;
import com.dmart.blockchain.exception.BadRequestException;
import com.dmart.blockchain.exception.ResourceNotFoundException;
import com.dmart.blockchain.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllActiveCategories() {
        return categoryRepository.findByActiveTrue().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategoriesAdmin() {
        return categoryRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getById(Long id) {
        Category c = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return toResponse(c);
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest req) {
        String slug = req.getSlug() != null && !req.getSlug().isBlank()
            ? req.getSlug().trim().toLowerCase().replaceAll("[^a-z0-9-]", "-")
            : req.getName().trim().toLowerCase().replaceAll("[^a-z0-9-]", "-");

        if (categoryRepository.existsBySlug(slug)) {
            throw new BadRequestException("Category slug already exists: " + slug);
        }

        Category category = Category.builder()
            .name(req.getName().trim())
            .slug(slug)
            .description(req.getDescription())
            .imageUrl(req.getImageUrl())
            .active(req.getActive() != null ? req.getActive() : true)
            .build();

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest req) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (req.getName() != null) category.setName(req.getName().trim());
        if (req.getDescription() != null) category.setDescription(req.getDescription());
        if (req.getImageUrl() != null) category.setImageUrl(req.getImageUrl());
        if (req.getActive() != null) category.setActive(req.getActive());

        if (req.getSlug() != null && !req.getSlug().isBlank()) {
            String slug = req.getSlug().trim().toLowerCase();
            if (categoryRepository.existsBySlugAndIdNot(slug, id)) {
                throw new BadRequestException("Slug is already used by another category");
            }
            category.setSlug(slug);
        }

        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public void deactivateCategory(Long id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        category.setActive(false);
        categoryRepository.save(category);
    }

    public CategoryResponse toResponse(Category c) {
        return CategoryResponse.builder()
            .id(c.getId())
            .name(c.getName())
            .slug(c.getSlug())
            .description(c.getDescription())
            .imageUrl(c.getImageUrl())
            .active(c.isActive())
            .createdAt(c.getCreatedAt())
            .build();
    }
}
