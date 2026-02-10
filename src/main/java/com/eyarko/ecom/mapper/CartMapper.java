package com.eyarko.ecom.mapper;

import com.eyarko.ecom.dto.CartItemResponse;
import com.eyarko.ecom.dto.CartResponse;
import com.eyarko.ecom.entity.Cart;
import com.eyarko.ecom.entity.CartItem;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class CartMapper {
    private CartMapper() {
    }

    public static CartResponse toResponse(Cart cart) {
        List<CartItem> items = Optional.ofNullable(cart.getItems()).orElse(Collections.emptyList());
        List<CartItemResponse> itemResponses = items.stream()
            .map(CartMapper::toItemResponse)
            .collect(Collectors.toList());
        BigDecimal totalAmount = items.stream()
            .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalItems = items.stream().mapToInt(CartItem::getQuantity).sum();

        return CartResponse.builder()
            .id(cart.getId())
            .userId(cart.getUser() != null ? cart.getUser().getId() : null)
            .items(itemResponses)
            .totalItems(totalItems)
            .totalAmount(totalAmount)
            .updatedAt(cart.getUpdatedAt())
            .build();
    }

    private static CartItemResponse toItemResponse(CartItem item) {
        BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return CartItemResponse.builder()
            .id(item.getId())
            .productId(item.getProduct().getId())
            .productName(item.getProduct().getName())
            .imageUrl(item.getProduct().getImageUrl())
            .unitPrice(item.getUnitPrice())
            .quantity(item.getQuantity())
            .lineTotal(lineTotal)
            .build();
    }
}

