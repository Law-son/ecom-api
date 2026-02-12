package com.eyarko.ecom.service;

import com.eyarko.ecom.dto.CategoryRequest;
import com.eyarko.ecom.dto.CategoryResponse;
import com.eyarko.ecom.entity.Category;
import com.eyarko.ecom.mapper.CategoryMapper;
import com.eyarko.ecom.repository.CartItemRepository;
import com.eyarko.ecom.repository.CategoryRepository;
import com.eyarko.ecom.repository.OrderItemRepository;
import com.eyarko.ecom.repository.ProductRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
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
    private final CartItemRepository cartItemRepository;
    private final OrderItemRepository orderItemRepository;

    public CategoryService(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            CartItemRepository cartItemRepository,
            OrderItemRepository orderItemRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderItemRepository = orderItemRepository;
    }

    /**
     * Creates a category.
     *
     * @param request category payload
     * @return created category
     */
    @CacheEvict(value = "categories", allEntries = true)
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
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAll(Sort.by("name")).stream()
            .map(CategoryMapper::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Deletes a category by id and all its related products (and their cart/order line items).
     *
     * @param id category id
     */
    @Transactional
    @CacheEvict(value = {"categories", "products"}, allEntries = true)
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found");
        }
        List<Long> productIds = productRepository.findByCategory_Id(id, Pageable.unpaged())
                .getContent().stream()
                .map(p -> p.getId())
                .collect(Collectors.toList());
        if (!productIds.isEmpty()) {
            cartItemRepository.deleteByProduct_IdIn(productIds);
            orderItemRepository.deleteByProduct_IdIn(productIds);
        }
        productRepository.deleteByCategory_Id(id);
        categoryRepository.deleteById(id);
    }
}


