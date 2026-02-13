package com.eyarko.ecom.service;

import com.eyarko.ecom.dto.CartItemRequest;
import com.eyarko.ecom.dto.CartItemUpdateRequest;
import com.eyarko.ecom.dto.CartResponse;
import com.eyarko.ecom.entity.Cart;
import com.eyarko.ecom.entity.CartItem;
import com.eyarko.ecom.entity.Product;
import com.eyarko.ecom.mapper.CartMapper;
import com.eyarko.ecom.repository.CartItemRepository;
import com.eyarko.ecom.repository.CartRepository;
import com.eyarko.ecom.repository.ProductRepository;
import com.eyarko.ecom.repository.UserRepository;
import com.eyarko.ecom.util.SecurityUtil;
import java.util.ArrayList;
import java.util.Optional;
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
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(
        CartRepository cartRepository,
        CartItemRepository cartItemRepository,
        ProductRepository productRepository,
        UserRepository userRepository
    ) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
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

        Optional<CartItem> existing = cartItemRepository.findByCart_IdAndProduct_Id(cart.getId(), product.getId());
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
        } else {
            CartItem item = CartItem.builder()
                .cart(cart)
                .product(product)
                .quantity(request.getQuantity())
                .unitPrice(product.getPrice())
                .build();
            cart.getItems().add(item);
        }
        Cart saved = cartRepository.save(cart);
        return CartMapper.toResponse(saved);
    }

    @Transactional
    public CartResponse updateItem(Long productId, CartItemUpdateRequest request) {
        Long userId = SecurityUtil.requireCurrentUserId();
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findByCart_IdAndProduct_Id(cart.getId(), productId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));
        item.setQuantity(request.getQuantity());
        Cart saved = cartRepository.save(cart);
        return CartMapper.toResponse(saved);
    }

    @Transactional
    public CartResponse removeItem(Long productId) {
        Long userId = SecurityUtil.requireCurrentUserId();
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findByCart_IdAndProduct_Id(cart.getId(), productId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found"));
        cart.getItems().remove(item);
        Cart saved = cartRepository.save(cart);
        return CartMapper.toResponse(saved);
    }

    @Transactional
    public CartResponse clearCart() {
        Long userId = SecurityUtil.requireCurrentUserId();
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        Cart saved = cartRepository.save(cart);
        return CartMapper.toResponse(saved);
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUser_Id(userId)
            .orElseGet(() -> {
                Cart cart = new Cart();
                cart.setUser(userRepository.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")));
                cart.setItems(new ArrayList<>());
                return cartRepository.save(cart);
            });
    }
}

