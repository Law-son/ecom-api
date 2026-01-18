package com.eyarko.ecom.graphql;

import com.eyarko.ecom.dto.CategoryRequest;
import com.eyarko.ecom.dto.CategoryResponse;
import com.eyarko.ecom.dto.OrderCreateRequest;
import com.eyarko.ecom.dto.OrderResponse;
import com.eyarko.ecom.dto.ProductRequest;
import com.eyarko.ecom.dto.ProductResponse;
import com.eyarko.ecom.dto.ReviewCreateRequest;
import com.eyarko.ecom.dto.ReviewResponse;
import com.eyarko.ecom.service.CategoryService;
import com.eyarko.ecom.service.OrderService;
import com.eyarko.ecom.service.ProductService;
import com.eyarko.ecom.service.ReviewService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

@Controller
public class MutationController {
    private final ProductService productService;
    private final CategoryService categoryService;
    private final OrderService orderService;
    private final ReviewService reviewService;

    public MutationController(
        ProductService productService,
        CategoryService categoryService,
        OrderService orderService,
        ReviewService reviewService
    ) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.orderService = orderService;
        this.reviewService = reviewService;
    }

    @MutationMapping
    public ProductResponse createProduct(@Argument ProductRequest input) {
        return productService.createProduct(input);
    }

    @MutationMapping
    public CategoryResponse createCategory(@Argument CategoryRequest input) {
        return categoryService.createCategory(input);
    }

    @MutationMapping
    public OrderResponse createOrder(@Argument OrderCreateRequest input) {
        return orderService.createOrder(input);
    }

    @MutationMapping
    public ReviewResponse addReview(@Argument ReviewCreateRequest input) {
        return reviewService.createReview(input);
    }
}


