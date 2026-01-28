package com.eyarko.ecom.service;

import com.eyarko.ecom.dto.ProductRequest;
import com.eyarko.ecom.dto.ProductResponse;
import com.eyarko.ecom.entity.Category;
import com.eyarko.ecom.entity.Product;
import com.eyarko.ecom.mapper.ProductMapper;
import com.eyarko.ecom.repository.CategoryRepository;
import com.eyarko.ecom.repository.ProductRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Product business logic.
 */
@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Creates a new product.
     *
     * @param request product payload
     * @return created product
     */
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse createProduct(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
        Product product = Product.builder()
            .category(category)
            .name(request.getName())
            .description(request.getDescription())
            .price(request.getPrice())
            .imageUrl(request.getImageUrl())
            .build();
        return ProductMapper.toResponse(productRepository.save(product));
    }

    /**
     * Updates an existing product.
     *
     * @param id product id
     * @param request product payload
     * @return updated product
     */
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
            product.setCategory(category);
        }
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setImageUrl(request.getImageUrl());
        return ProductMapper.toResponse(productRepository.save(product));
    }

    /**
     * Retrieves a product by id.
     *
     * @param id product id
     * @return product details
     */
    @Cacheable(value = "products", key = "'product:' + #id")
    public ProductResponse getProduct(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        return ProductMapper.toResponse(product);
    }

    /**
     * Lists all products without pagination.
     *
     * @return list of products
     */
    @Cacheable(value = "products", key = "'all'")
    public List<ProductResponse> listAllProducts() {
        return productRepository.findAll()
            .stream()
            .map(ProductMapper::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Lists products with optional filtering and pagination.
     *
     * @param categoryId optional category filter
     * @param search optional search query
     * @param pageable paging and sorting options
     * @return list of products
     */
    @Cacheable(
        value = "products",
        key = "'list:' + #categoryId + ':' + #search + ':' + #pageable.pageNumber + ':' + #pageable.pageSize "
            + "+ ':' + #pageable.sort.toString()"
    )
    public List<ProductResponse> listProducts(Long categoryId, String search, Pageable pageable) {
        List<Product> products;
        if (categoryId != null && search != null && !search.isBlank()) {
            products = productRepository.findByCategoryIdAndNameContainingIgnoreCase(categoryId, search, pageable);
        } else if (categoryId != null) {
            products = productRepository.findByCategoryId(categoryId, pageable);
        } else if (search != null && !search.isBlank()) {
            products = productRepository.findByNameContainingIgnoreCaseOrCategory_NameContainingIgnoreCase(
                search,
                search,
                pageable
            );
        } else {
            products = productRepository.findAll(pageable);
        }
        return products.stream().map(ProductMapper::toResponse).collect(Collectors.toList());
    }

    /**
     * Deletes a product by id.
     *
     * @param id product id
     */
    @CacheEvict(value = "products", allEntries = true)
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        productRepository.deleteById(id);
    }
}


