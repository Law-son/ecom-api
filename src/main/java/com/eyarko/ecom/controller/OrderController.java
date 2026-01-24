package com.eyarko.ecom.controller;

import com.eyarko.ecom.dto.ApiResponse;
import com.eyarko.ecom.dto.OrderCreateRequest;
import com.eyarko.ecom.dto.OrderResponse;
import com.eyarko.ecom.dto.OrderStatusUpdateRequest;
import com.eyarko.ecom.service.OrderService;
import com.eyarko.ecom.util.ResponseUtil;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Order management endpoints.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Creates a new order.
     *
     * @param request order payload
     * @return created order
     */
    @PostMapping
    public ApiResponse<OrderResponse> createOrder(@Valid @RequestBody OrderCreateRequest request) {
        return ResponseUtil.success("Order created", orderService.createOrder(request));
    }

    /**
     * Retrieves an order by id.
     *
     * @param id order id
     * @return order details
     */
    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> getOrder(@PathVariable Long id) {
        return ResponseUtil.success("Order retrieved", orderService.getOrder(id));
    }

    /**
     * Lists orders, optionally filtered by user id.
     *
     * @param userId optional user id
     * @param page page index
     * @param size page size
     * @param sortBy field to sort by
     * @param sortDir sort direction
     * @return list of orders
     */
    @GetMapping
    public ApiResponse<List<OrderResponse>> listOrders(
        @RequestParam(required = false) Long userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "orderDate") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(parseDirection(sortDir), sortBy));
        return ResponseUtil.success("Orders retrieved", orderService.listOrders(userId, pageable));
    }

    /**
     * Updates the status of an order using PATCH.
     *
     * @param id order id
     * @param request status update payload
     * @return updated order
     */
    @PatchMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateOrderStatus(
        @PathVariable Long id,
        @Valid @RequestBody OrderStatusUpdateRequest request
    ) {
        return ResponseUtil.success("Order status updated", orderService.updateOrderStatus(id, request));
    }

    /**
     * Updates the status of an order using PUT.
     *
     * @param id order id
     * @param request status update payload
     * @return updated order
     */
    @PutMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateOrderStatusPut(
        @PathVariable Long id,
        @Valid @RequestBody OrderStatusUpdateRequest request
    ) {
        return ResponseUtil.success("Order status updated", orderService.updateOrderStatus(id, request));
    }

    private Sort.Direction parseDirection(String sortDir) {
        try {
            return Sort.Direction.fromString(sortDir);
        } catch (IllegalArgumentException ex) {
            return Sort.Direction.DESC;
        }
    }
}

