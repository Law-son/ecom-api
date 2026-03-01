package com.eyarko.ecom.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
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
import com.eyarko.ecom.security.UserPrincipal;
import io.micrometer.core.instrument.Timer;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @Mock
    private CacheManager cacheManager;

    @Mock
    private InventoryLockManager inventoryLockManager;

    @Mock
    private ApplicationMetricsService applicationMetricsService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_throwsWhenInsufficientInventory() {
        User user = User.builder().id(1L).email("user@example.com").passwordHash("hash").build();
        Product product = Product.builder().id(10L).price(BigDecimal.TEN).build();
        Inventory inventory = Inventory.builder().product(product).quantity(1).build();
        Timer.Sample sample = org.mockito.Mockito.mock(Timer.Sample.class);

        UserPrincipal principal = UserPrincipal.builder()
            .id(1L)
            .email("user@example.com")
            .passwordHash("hash")
            .authorities(List.of())
            .build();
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(inventoryRepository.findByProductIdForUpdate(10L)).thenReturn(Optional.of(inventory));
        when(applicationMetricsService.startTimer()).thenReturn(sample);
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(1);
            action.run();
            return null;
        }).when(inventoryLockManager).withProductLock(eq(10L), any(Runnable.class));

        OrderCreateRequest request = OrderCreateRequest.builder()
            .items(List.of(OrderItemRequest.builder().productId(10L).quantity(2).build()))
            .build();

        try {
            assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResponseStatusException.class);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}

