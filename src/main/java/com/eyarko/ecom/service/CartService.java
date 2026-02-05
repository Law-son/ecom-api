package com.eyarko.ecom.service;

import com.eyarko.ecom.dto.CartItemRequest;
import com.eyarko.ecom.dto.CartItemUpdateRequest;
import com.eyarko.ecom.dto.CartResponse;
import com.eyarko.ecom.entity.Cart;
import com.eyarko.ecom.entity.Product;
import com.eyarko.ecom.mapper.CartMapper;
import com.eyarko.ecom.repository.CartRepository;
import com.eyarko.ecom.repository.ProductRepository;
import com.eyarko.ecom.util.SecurityUtil;
import java.util.ArrayList;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Cart business logic for the current user.
 */
@Service
public class CartService {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartService(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    public CartResponse getCart() {
        Long userId = SecurityUtil.requireCurrentUserId();
        Cart cart = getOrCreateCart(userId);
        return CartMapper.toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(CartItemRequest request) {
        Long userId = SecurityUtil.requireCurrentUserId();
        Cart cart = getOrCreateCart(userId);
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));

        cartRepository.findItem(cart.getId(), product.getId())
            .ifPresentOrElse(existing -> {
                int newQuantity = existing.getQuantity() + request.getQuantity();
                cartRepository.updateItemQuantity(cart.getId(), product.getId(), newQuantity);
            }, () -> cartRepository.insertItem(
                cart.getId(),
                product.getId(),
                request.getQuantity(),
                product.getPrice()
            ));

        cartRepository.touchCart(cart.getId());
        return CartMapper.toResponse(cartRepository.findByUserId(userId).orElse(cart));
    }

    @Transactional
    public CartResponse updateItem(Long productId, CartItemUpdateRequest request) {
        Long userId = SecurityUtil.requireCurrentUserId();
        Cart cart = getOrCreateCart(userId);
        if (cartRepository.findItem(cart.getId(), productId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found");
        }
        cartRepository.updateItemQuantity(cart.getId(), productId, request.getQuantity());
        cartRepository.touchCart(cart.getId());
        return CartMapper.toResponse(cartRepository.findByUserId(userId).orElse(cart));
    }

    @Transactional
    public CartResponse removeItem(Long productId) {
        Long userId = SecurityUtil.requireCurrentUserId();
        Cart cart = getOrCreateCart(userId);
        if (cartRepository.findItem(cart.getId(), productId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found");
        }
        cartRepository.deleteItem(cart.getId(), productId);
        cartRepository.touchCart(cart.getId());
        return CartMapper.toResponse(cartRepository.findByUserId(userId).orElse(cart));
    }

    @Transactional
    public CartResponse clearCart() {
        Long userId = SecurityUtil.requireCurrentUserId();
        Cart cart = getOrCreateCart(userId);
        cartRepository.clearCart(cart.getId());
        cartRepository.touchCart(cart.getId());
        Cart cleared = Cart.builder()
            .id(cart.getId())
            .userId(userId)
            .createdAt(cart.getCreatedAt())
            .updatedAt(cart.getUpdatedAt())
            .items(new ArrayList<>())
            .build();
        return CartMapper.toResponse(cleared);
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
            .orElseGet(() -> cartRepository.createCart(userId));
    }
}

