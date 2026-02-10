package com.eyarko.ecom.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.eyarko.ecom.dto.OrderCreateRequest;
import com.eyarko.ecom.dto.OrderItemRequest;
import com.eyarko.ecom.entity.Inventory;
import com.eyarko.ecom.entity.Product;
import com.eyarko.ecom.entity.User;
import com.eyarko.ecom.repository.InventoryRepository;
import com.eyarko.ecom.repository.OrderRepository;
import com.eyarko.ecom.repository.ProductRepository;
import com.eyarko.ecom.repository.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_throwsWhenInsufficientInventory() {
        User user = User.builder().id(1L).build();
        Product product = Product.builder().id(10L).price(BigDecimal.TEN).build();
        Inventory inventory = Inventory.builder().product(product).quantity(1).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProduct_Id(10L)).thenReturn(Optional.of(inventory));

        OrderCreateRequest request = OrderCreateRequest.builder()
            .userId(1L)
            .items(List.of(OrderItemRequest.builder().productId(10L).quantity(2).build()))
            .build();

        assertThatThrownBy(() -> orderService.createOrder(request))
            .isInstanceOf(ResponseStatusException.class);
    }
}

