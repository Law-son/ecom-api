package com.eyarko.ecom.service;

import com.eyarko.ecom.dto.PagedResponse;
import com.eyarko.ecom.dto.ProductRequest;
import com.eyarko.ecom.dto.ProductResponse;
import com.eyarko.ecom.entity.Category;
import com.eyarko.ecom.entity.Product;
import com.eyarko.ecom.mapper.ProductMapper;
import com.eyarko.ecom.repository.CategoryRepository;
import com.eyarko.ecom.repository.InventoryRepository;
import com.eyarko.ecom.repository.ProductRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Product business logic.
 */
@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryRepository inventoryRepository;

    public ProductService(
        ProductRepository productRepository,
        CategoryRepository categoryRepository,
        InventoryRepository inventoryRepository
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * Creates a new product.
     *
     * @param request product payload
     * @return created product
     */
    @CacheEvict(value = "products", allEntries = true)
    @Transactional
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
    @Transactional
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
    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        int qty = inventoryRepository.findByProduct_Id(id)
            .map(inv -> inv.getQuantity() != null ? inv.getQuantity() : 0)
            .orElse(0);
        return ProductMapper.toResponse(product, qty);
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
    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> listProducts(Long categoryId, String search, Pageable pageable) {
        Page<Product> page;
        if (categoryId != null && search != null && !search.isBlank()) {
            page = productRepository.findByCategory_IdAndNameContainingIgnoreCase(categoryId, search, pageable);
        } else if (categoryId != null) {
            page = productRepository.findByCategory_Id(categoryId, pageable);
        } else if (search != null && !search.isBlank()) {
            page = productRepository.searchByNameOrCategory(search, pageable);
        } else {
            page = productRepository.findAllProducts(pageable);
        }
        List<Product> products = page.getContent();
        List<Long> ids = products.stream().map(Product::getId).collect(Collectors.toList());
        Map<Long, Integer> quantities = loadQuantities(ids);
        List<ProductResponse> items = products.stream()
            .map(p -> ProductMapper.toResponse(p, quantities.getOrDefault(p.getId(), 0)))
            .collect(Collectors.toList());
        return PagedResponse.<ProductResponse>builder()
            .items(items)
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .hasNext(page.hasNext())
            .hasPrevious(page.hasPrevious())
            .build();
    }

    /**
     * Deletes a product by id.
     *
     * @param id product id
     */
    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        productRepository.deleteById(id);
    }

    private Map<Long, Integer> loadQuantities(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        return inventoryRepository.findQuantitiesByProductIds(productIds).stream()
            .collect(Collectors.toMap(
                view -> view.getProductId(),
                view -> view.getQuantity() == null ? 0 : view.getQuantity()
            ));
    }
}


