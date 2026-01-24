package com.eyarko.ecom.controller;

import com.eyarko.ecom.dto.ApiResponse;
import com.eyarko.ecom.dto.CategoryRequest;
import com.eyarko.ecom.dto.CategoryResponse;
import com.eyarko.ecom.service.CategoryService;
import com.eyarko.ecom.util.ResponseUtil;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Category management endpoints.
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Creates a new category.
     *
     * @param request category payload
     * @return created category
     */
    @PostMapping
    public ApiResponse<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        return ResponseUtil.success("Category created", categoryService.createCategory(request));
    }

    /**
     * Updates an existing category.
     *
     * @param id category id
     * @param request category payload
     * @return updated category
     */
    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> updateCategory(
        @PathVariable Long id,
        @Valid @RequestBody CategoryRequest request
    ) {
        return ResponseUtil.success("Category updated", categoryService.updateCategory(id, request));
    }

    /**
     * Retrieves a category by id.
     *
     * @param id category id
     * @return category details
     */
    @GetMapping("/{id}")
    public ApiResponse<CategoryResponse> getCategory(@PathVariable Long id) {
        return ResponseUtil.success("Category retrieved", categoryService.getCategory(id));
    }

    /**
     * Lists all categories.
     *
     * @return list of categories
     */
    @GetMapping
    public ApiResponse<List<CategoryResponse>> listCategories() {
        return ResponseUtil.success("Categories retrieved", categoryService.listCategories());
    }

    /**
     * Deletes a category by id.
     *
     * @param id category id
     * @return success response
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseUtil.success("Category deleted", null);
    }
}

