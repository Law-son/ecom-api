package com.eyarko.ecom.service;

import com.eyarko.ecom.dto.OrderCreateRequest;
import com.eyarko.ecom.dto.OrderItemRequest;
import com.eyarko.ecom.dto.OrderResponse;
import com.eyarko.ecom.entity.Order;
import com.eyarko.ecom.entity.OrderItem;
import com.eyarko.ecom.entity.Product;
import com.eyarko.ecom.entity.User;
import com.eyarko.ecom.mapper.OrderMapper;
import com.eyarko.ecom.repository.OrderRepository;
import com.eyarko.ecom.repository.ProductRepository;
import com.eyarko.ecom.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public OrderService(
        OrderRepository orderRepository,
        UserRepository userRepository,
        ProductRepository productRepository
    ) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    public OrderResponse createOrder(OrderCreateRequest request) {
        User user = userRepository.findById(request.getUserId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        Order order = Order.builder()
            .user(user)
            .build();

        List<OrderItem> items = request.getItems().stream()
            .map(item -> toOrderItem(order, item))
            .collect(Collectors.toList());

        order.setItems(items);
        order.setTotalAmount(calculateTotal(items));

        return OrderMapper.toResponse(orderRepository.save(order));
    }

    public OrderResponse getOrder(Long id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return OrderMapper.toResponse(order);
    }

    public List<OrderResponse> listOrders(Long userId, Pageable pageable) {
        Page<Order> page = (userId == null)
            ? orderRepository.findAll(pageable)
            : orderRepository.findByUserId(userId, pageable);
        return page.stream().map(OrderMapper::toResponse).collect(Collectors.toList());
    }

    private OrderItem toOrderItem(Order order, OrderItemRequest request) {
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        return OrderItem.builder()
            .order(order)
            .product(product)
            .quantity(request.getQuantity())
            .priceAtTime(product.getPrice())
            .build();
    }

    private BigDecimal calculateTotal(List<OrderItem> items) {
        return items.stream()
            .map(item -> item.getPriceAtTime().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

