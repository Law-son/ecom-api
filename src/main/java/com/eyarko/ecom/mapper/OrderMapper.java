package com.eyarko.ecom.mapper;

import com.eyarko.ecom.dto.OrderItemResponse;
import com.eyarko.ecom.dto.OrderResponse;
import com.eyarko.ecom.entity.Order;
import com.eyarko.ecom.entity.OrderItem;
import com.eyarko.ecom.entity.Product;
import com.eyarko.ecom.entity.User;
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
            .userId(safeUserId(order))
            .status(order.getStatus())
            .totalAmount(order.getTotalAmount())
            .orderDate(order.getOrderDate())
            .items(items)
            .build();
    }

    private static Long safeUserId(Order order) {
        try {
            User user = order.getUser();
            return user != null ? user.getId() : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private static OrderItemResponse toItemResponse(OrderItem item) {
        Product product;
        try {
            product = item.getProduct();
        } catch (Exception ex) {
            product = null;
        }
        return OrderItemResponse.builder()
            .productId(product != null ? product.getId() : null)
            .productName(product != null ? product.getName() : null)
            .quantity(item.getQuantity())
            .priceAtTime(item.getPriceAtTime())
            .build();
    }
}


