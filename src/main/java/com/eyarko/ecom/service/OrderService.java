package com.eyarko.ecom.service;

import com.eyarko.ecom.dto.OrderCreateRequest;
import com.eyarko.ecom.dto.OrderItemRequest;
import com.eyarko.ecom.dto.OrderResponse;
import com.eyarko.ecom.dto.OrderStatusUpdateRequest;
import com.eyarko.ecom.entity.Inventory;
import com.eyarko.ecom.entity.Order;
import com.eyarko.ecom.entity.OrderItem;
import com.eyarko.ecom.entity.Product;
import com.eyarko.ecom.entity.User;
import com.eyarko.ecom.mapper.OrderMapper;
import com.eyarko.ecom.repository.InventoryRepository;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public OrderService(
        OrderRepository orderRepository,
        UserRepository userRepository,
        ProductRepository productRepository,
        InventoryRepository inventoryRepository
    ) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Order items are required");
        }
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

        items.forEach(this::reserveInventory);

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

    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        order.setStatus(request.getStatus());
        return OrderMapper.toResponse(orderRepository.save(order));
    }

    private OrderItem toOrderItem(Order order, OrderItemRequest request) {
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
        return OrderItem.builder()
            .order(order)
            .product(product)
            .quantity(request.getQuantity())
            .unitPrice(product.getPrice())
            .priceAtTime(product.getPrice())
            .build();
    }

    private void reserveInventory(OrderItem item) {
        Inventory inventory = inventoryRepository.findByProductId(item.getProduct().getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inventory not found"));
        int remaining = inventory.getQuantity() - item.getQuantity();
        if (remaining < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock");
        }
        inventory.setQuantity(remaining);
        inventoryRepository.save(inventory);
    }

    private BigDecimal calculateTotal(List<OrderItem> items) {
        return items.stream()
            .map(item -> item.getPriceAtTime().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}


