package com.eyarko.ecom.mapper;

import com.eyarko.ecom.dto.OrderItemResponse;
import com.eyarko.ecom.dto.OrderResponse;
import com.eyarko.ecom.entity.Order;
import com.eyarko.ecom.entity.OrderItem;
import com.eyarko.ecom.entity.Product;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class OrderMapper {
    private OrderMapper() {
    }

    public static OrderResponse toResponse(Order order) {
        if (order == null) {
            return null;
        }
        List<OrderItem> orderItems = order.getItems();
        List<OrderItemResponse> items = (orderItems == null ? Collections.<OrderItem>emptyList() : orderItems)
            .stream()
            .map(OrderMapper::toItemResponse)
            .collect(Collectors.toList());

        return OrderResponse.builder()
            .id(order.getId())
            .userId(order.getUser() != null ? order.getUser().getId() : null)
            .status(order.getStatus())
            .totalAmount(order.getTotalAmount())
            .orderDate(order.getOrderDate())
            .items(items)
            .build();
    }

    private static OrderItemResponse toItemResponse(OrderItem item) {
        Product product = item.getProduct();
        return OrderItemResponse.builder()
            .productId(product != null ? product.getId() : null)
            .productName(product != null ? product.getName() : null)
            .quantity(item.getQuantity())
            .priceAtTime(item.getPriceAtTime())
            .build();
    }
}


