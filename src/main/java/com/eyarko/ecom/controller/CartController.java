package com.eyarko.ecom.controller;

import com.eyarko.ecom.dto.ApiResponse;
import com.eyarko.ecom.dto.CartItemRequest;
import com.eyarko.ecom.dto.CartItemUpdateRequest;
import com.eyarko.ecom.dto.CartResponse;
import com.eyarko.ecom.service.CartService;
import com.eyarko.ecom.util.ResponseUtil;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cart endpoints for authenticated users.
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ApiResponse<CartResponse> getCart() {
        return ResponseUtil.success("Cart retrieved", cartService.getCart());
    }

    @PostMapping("/items")
    public ApiResponse<CartResponse> addItem(@Valid @RequestBody CartItemRequest request) {
        return ResponseUtil.success("Item added to cart", cartService.addItem(request));
    }

    @PatchMapping("/items/{productId}")
    public ApiResponse<CartResponse> updateItem(
        @PathVariable Long productId,
        @Valid @RequestBody CartItemUpdateRequest request
    ) {
        return ResponseUtil.success("Cart item updated", cartService.updateItem(productId, request));
    }

    @DeleteMapping("/items/{productId}")
    public ApiResponse<CartResponse> removeItem(@PathVariable Long productId) {
        return ResponseUtil.success("Cart item removed", cartService.removeItem(productId));
    }

    @DeleteMapping
    public ApiResponse<CartResponse> clearCart() {
        return ResponseUtil.success("Cart cleared", cartService.clearCart());
    }
}

