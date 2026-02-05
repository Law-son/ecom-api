package com.eyarko.ecom.service;

import com.eyarko.ecom.dto.OrderCreateRequest;
import com.eyarko.ecom.dto.OrderItemRequest;
import com.eyarko.ecom.dto.OrderResponse;
import com.eyarko.ecom.dto.OrderStatusUpdateRequest;
import com.eyarko.ecom.entity.Inventory;
import com.eyarko.ecom.entity.Order;
import com.eyarko.ecom.entity.OrderItem;
import com.eyarko.ecom.entity.Product;
import com.eyarko.ecom.entity.OrderStatus;
import com.eyarko.ecom.entity.User;
import com.eyarko.ecom.mapper.OrderMapper;
import com.eyarko.ecom.repository.InventoryRepository;
import com.eyarko.ecom.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.eyarko.ecom.repository.OrderRepository;
import com.eyarko.ecom.repository.ProductRepository;
import com.eyarko.ecom.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Order business logic.
 */
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

    /**
     * Creates an order and reserves inventory.
     *
     * @param request order payload
     * @return created order
     */
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

    /**
     * Retrieves an order by id.
     *
     * @param id order id
     * @return order details
     */
    public OrderResponse getOrder(Long id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return OrderMapper.toResponse(order);
    }

    /**
     * Lists orders with optional user filtering.
     *
     * @param userId optional user id
     * @param pageable paging and sorting options
     * @return list of orders
     */
    public List<OrderResponse> listOrders(Long userId, Pageable pageable) {
        List<Order> orders = (userId == null)
            ? orderRepository.findAll(pageable)
            : orderRepository.findByUserId(userId, pageable);
        return orders.stream().map(OrderMapper::toResponse).collect(Collectors.toList());
    }

    /**
     * Updates the status of an order. Resolves the current user from JWT/session.
     * Admin: allowed to set any status. Customer: only allowed to set RECEIVED when
     * the order is DELIVERED and the order belongs to the current user.
     *
     * @param id order id
     * @param request status update payload
     * @return updated order
     */
    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatusUpdateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
            || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authentication required");
        }
        Long currentUserId = principal.getId();
        boolean isAdmin = authentication.getAuthorities().stream()
            .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));

        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cancelled orders cannot be updated");
        }

        if (!isAdmin) {
            Long orderUserId = order.getUser() != null ? order.getUser().getId() : null;
            if (orderUserId == null || !orderUserId.equals(currentUserId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the order owner");
            }
            if (request.getStatus() != OrderStatus.RECEIVED) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Customers can only set status to RECEIVED");
            }
            if (order.getStatus() != OrderStatus.DELIVERED) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Can only mark as RECEIVED when order status is DELIVERED");
            }
        }

        if (request.getStatus() == OrderStatus.CANCELLED) {
            restockInventory(order);
        }

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

    private void restockInventory(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return;
        }
        for (OrderItem item : order.getItems()) {
            Inventory inventory = inventoryRepository.findByProductId(item.getProduct().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Inventory not found"));
            inventory.setQuantity(inventory.getQuantity() + item.getQuantity());
            inventoryRepository.save(inventory);
        }
    }

    private BigDecimal calculateTotal(List<OrderItem> items) {
        return items.stream()
            .map(item -> item.getPriceAtTime().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}


