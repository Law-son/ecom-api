package com.eyarko.ecom.service;

import com.eyarko.ecom.dto.CategoryRequest;
import com.eyarko.ecom.dto.CategoryResponse;
import com.eyarko.ecom.entity.Category;
import com.eyarko.ecom.mapper.CategoryMapper;
import com.eyarko.ecom.repository.CategoryRepository;
import com.eyarko.ecom.repository.ProductRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Category business logic.
 */
@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(
            CategoryRepository categoryRepository,
            ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    /**
     * Creates a category.
     *
     * @param request category payload
     * @return created category
     */
    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
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
    @CacheEvict(value = "categories", allEntries = true)
    @Transactional
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
    @Cacheable(value = "categories", key = "'category:' + #id")
    @Transactional(readOnly = true)
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
    @Cacheable(value = "categories", key = "'all'")
    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAllCategories(Sort.by("name")).stream()
            .map(CategoryMapper::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Deletes a category by id.
     *
     * @param id category id
     */
    @Transactional
    @CacheEvict(value = {"categories", "products"}, allEntries = true)
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
        }
        if (productRepository.existsByCategory_Id(id)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Category cannot be deleted because it has products"
            );
        }
        categoryRepository.deleteById(id);
    }
}


