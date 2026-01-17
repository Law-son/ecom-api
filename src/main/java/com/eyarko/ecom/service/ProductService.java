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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

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

    public ProductResponse getProduct(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        return ProductMapper.toResponse(product);
    }

    public List<ProductResponse> listProducts(Long categoryId, String search, Pageable pageable) {
        Page<Product> page;
        if (categoryId != null && search != null && !search.isBlank()) {
            page = productRepository.findByCategoryIdAndNameContainingIgnoreCase(categoryId, search, pageable);
        } else if (categoryId != null) {
            page = productRepository.findByCategoryId(categoryId, pageable);
        } else if (search != null && !search.isBlank()) {
            page = productRepository.findByNameContainingIgnoreCase(search, pageable);
        } else {
            page = productRepository.findAll(pageable);
        }
        return page.stream().map(ProductMapper::toResponse).collect(Collectors.toList());
    }

    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }
        productRepository.deleteById(id);
    }
}

