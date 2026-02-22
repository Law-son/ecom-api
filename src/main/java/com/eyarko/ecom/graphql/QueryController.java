package com.eyarko.ecom.graphql;

import com.eyarko.ecom.dto.CategoryResponse;
import com.eyarko.ecom.dto.OrderResponse;
import com.eyarko.ecom.dto.ProductResponse;
import com.eyarko.ecom.dto.ReviewResponse;
import com.eyarko.ecom.dto.UserResponse;
import com.eyarko.ecom.service.CategoryService;
import com.eyarko.ecom.service.OrderService;
import com.eyarko.ecom.service.ProductService;
import com.eyarko.ecom.service.ReviewService;
import com.eyarko.ecom.service.UserService;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * GraphQL queries for core resources.
 */
@Controller
public class QueryController {
    private final ProductService productService;
    private final CategoryService categoryService;
    private final UserService userService;
    private final OrderService orderService;
    private final ReviewService reviewService;

    public QueryController(
        ProductService productService,
        CategoryService categoryService,
        UserService userService,
        OrderService orderService,
        ReviewService reviewService
    ) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.userService = userService;
        this.orderService = orderService;
        this.reviewService = reviewService;
    }

    /**
     * Retrieves a product by id via GraphQL.
     *
     * @param id product id
     * @return product details
     */
    @QueryMapping
    public ProductResponse productById(@Argument Long id) {
        return productService.getProduct(id);
    }

    /**
     * Lists products via GraphQL with optional filters.
     *
     * @param categoryId optional category filter
     * @param search optional search query
     * @param page page index
     * @param size page size
     * @param sortBy field to sort by
     * @param sortDir sort direction
     * @return list of products
     */
    @QueryMapping
    public List<ProductResponse> products(
        @Argument Long categoryId,
        @Argument String search,
        @Argument Integer page,
        @Argument Integer size,
        @Argument String sortBy,
        @Argument String sortDir
    ) {
        int resolvedPage = page == null ? 0 : page;
        int resolvedSize = size == null ? 20 : size;
        Sort.Direction direction = parseDirection(sortDir);
        Pageable pageable = PageRequest.of(resolvedPage, resolvedSize, Sort.by(direction, mapSortField(sortBy)));
        return productService.listProducts(categoryId, search, pageable).getItems();
    }

    /**
     * Lists all categories via GraphQL.
     *
     * @return list of categories
     */
    @QueryMapping
    public List<CategoryResponse> categories() {
        return categoryService.listCategories();
    }

    /**
     * Lists all users via GraphQL.
     *
     * @param page page index
     * @param size page size
     * @return list of users
     */
    @QueryMapping
    public List<UserResponse> users(@Argument Integer page, @Argument Integer size) {
        int resolvedPage = page == null ? 0 : page;
        int resolvedSize = size == null ? 20 : size;
        Pageable pageable = PageRequest.of(resolvedPage, resolvedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return userService.listUsers(pageable).getItems();
    }

    /**
     * Lists orders for a user via GraphQL.
     *
     * @param userId user id
     * @param page page index
     * @param size page size
     * @return list of orders
     */
    @QueryMapping
    public List<OrderResponse> ordersByUser(@Argument Long userId, @Argument Integer page, @Argument Integer size) {
        int resolvedPage = page == null ? 0 : page;
        int resolvedSize = size == null ? 20 : size;
        Pageable pageable = PageRequest.of(resolvedPage, resolvedSize, Sort.by(Sort.Direction.DESC, "orderDate"));
        return orderService.listOrders(userId, pageable).getItems();
    }

    /**
     * Lists reviews for a product via GraphQL.
     *
     * @param productId product id
     * @param page page index
     * @param size page size
     * @return list of reviews
     */
    @QueryMapping
    public List<ReviewResponse> reviewsByProduct(
        @Argument Long productId,
        @Argument Integer page,
        @Argument Integer size
    ) {
        int resolvedPage = page == null ? 0 : page;
        int resolvedSize = size == null ? 20 : size;
        Pageable pageable = PageRequest.of(resolvedPage, resolvedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return reviewService.listReviews(productId, null, pageable).getItems();
    }

    private Sort.Direction parseDirection(String sortDir) {
        try {
            return Sort.Direction.fromString(sortDir == null ? "asc" : sortDir);
        } catch (IllegalArgumentException ex) {
            return Sort.Direction.ASC;
        }
    }

    private String mapSortField(String sortBy) {
        if (sortBy == null) {
            return "name";
        }
        return switch (sortBy.toLowerCase()) {
            case "price" -> "price";
            case "rating" -> "avgRating";
            case "created" -> "createdAt";
            default -> "name";
        };
    }
}


