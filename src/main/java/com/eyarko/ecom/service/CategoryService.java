package com.eyarko.ecom.service;

import com.eyarko.ecom.dto.CategoryRequest;
import com.eyarko.ecom.dto.CategoryResponse;
import com.eyarko.ecom.entity.Category;
import com.eyarko.ecom.mapper.CategoryMapper;
import com.eyarko.ecom.repository.CategoryRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Category business logic.
 */
@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Creates a category.
     *
     * @param request category payload
     * @return created category
     */
    public CategoryResponse createCategory(CategoryRequest request) {
        Category category = Category.builder()
            .name(request.getName())
            .build();
        return CategoryMapper.toResponse(categoryRepository.save(category));
    }

    /**
     * Updates a category.
     *
     * @param id category id
     * @param request category payload
     * @return updated category
     */
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        category.setName(request.getName());
        return CategoryMapper.toResponse(categoryRepository.save(category));
    }

    /**
     * Retrieves a category by id.
     *
     * @param id category id
     * @return category details
     */
    public CategoryResponse getCategory(Long id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        return CategoryMapper.toResponse(category);
    }

    /**
     * Lists all categories.
     *
     * @return list of categories
     */
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAll().stream()
            .map(CategoryMapper::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Deletes a category by id.
     *
     * @param id category id
     */
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
        }
        categoryRepository.deleteById(id);
    }
}


