package com.dmart.blockchain.service;

import com.dmart.blockchain.blockchain.BlockchainService;
import com.dmart.blockchain.dto.request.OrderCreateRequest;
import com.dmart.blockchain.dto.response.OrderResponse;
import com.dmart.blockchain.entity.*;
import com.dmart.blockchain.exception.InsufficientStockException;
import com.dmart.blockchain.exception.InvalidStatusTransitionException;
import com.dmart.blockchain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private InventoryMovementRepository movementRepository;
    @Mock
    private AuditEventRepository auditEventRepository;
    @Mock
    private BlockchainService blockchainService;

    @InjectMocks
    private OrderService orderService;

    private User customer;
    private Product product;
    private CartItem cartItem;

    @BeforeEach
    void setUp() {
        customer = User.builder()
            .id(10L)
            .email("cust@example.com")
            .firstName("John")
            .lastName("Doe")
            .roles(Set.of(Role.builder().id(1L).name(Role.CUSTOMER).build()))
            .build();

        product = Product.builder()
            .id(100L)
            .sku("SKU-100")
            .name("Cooking Oil 1L")
            .price(BigDecimal.valueOf(120.00))
            .discountPercent(BigDecimal.ZERO)
            .stockQuantity(10)
            .active(true)
            .build();

        cartItem = CartItem.builder()
            .id(1L)
            .user(customer)
            .product(product)
            .quantity(2)
            .build();
    }

    @Test
    @DisplayName("Checkout validates stock, deducts inventory, creates order and triggers blockchain")
    void testCheckoutSuccess() {
        OrderCreateRequest req = new OrderCreateRequest();
        req.setDeliveryAddress("123 MG Road, Bengaluru");

        when(userRepository.findByEmail("cust@example.com")).thenReturn(Optional.of(customer));
        when(cartItemRepository.findByUserIdWithProduct(10L)).thenReturn(List.of(cartItem));

        Order savedOrder = Order.builder()
            .id(500L)
            .orderNumber("DMART-12345")
            .customer(customer)
            .deliveryAddress("123 MG Road, Bengaluru")
            .status(OrderStatus.PENDING)
            .subtotal(BigDecimal.valueOf(240.00))
            .totalAmount(BigDecimal.valueOf(240.00))
            .items(new ArrayList<>())
            .build();

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponse resp = orderService.checkout("cust@example.com", req);

        assertThat(resp).isNotNull();
        assertThat(resp.getId()).isEqualTo(500L);
        assertThat(product.getStockQuantity()).isEqualTo(8); // 10 - 2

        verify(productRepository).save(product);
        verify(movementRepository).save(any(InventoryMovement.class));
        verify(cartItemRepository).deleteByUserId(10L);
        verify(blockchainService).createOrderOnChainAsync(savedOrder);
    }

    @Test
    @DisplayName("Checkout fails when requested quantity exceeds available stock")
    void testCheckoutInsufficientStock() {
        cartItem.setQuantity(15); // product only has 10

        OrderCreateRequest req = new OrderCreateRequest();
        req.setDeliveryAddress("123 MG Road");

        when(userRepository.findByEmail("cust@example.com")).thenReturn(Optional.of(customer));
        when(cartItemRepository.findByUserIdWithProduct(10L)).thenReturn(List.of(cartItem));

        assertThatThrownBy(() -> orderService.checkout("cust@example.com", req))
            .isInstanceOf(InsufficientStockException.class)
            .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("Invalid status transition (e.g. PENDING to SHIPPED) throws exception")
    void testInvalidStatusTransition() {
        Order order = Order.builder()
            .id(1L)
            .status(OrderStatus.PENDING)
            .items(new ArrayList<>())
            .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, OrderStatus.SHIPPED, null, "admin@dmart.local"))
            .isInstanceOf(InvalidStatusTransitionException.class)
            .hasMessageContaining("Cannot transition order status");
    }

    @Test
    @DisplayName("Customer cannot access another customer's order")
    void testCustomerOrderAccessIsolation() {
        User anotherCustomer = User.builder()
            .id(99L)
            .email("intruder@example.com")
            .roles(Set.of(Role.builder().name(Role.CUSTOMER).build()))
            .build();

        Order order = Order.builder()
            .id(1L)
            .customer(customer) // belongs to customer ID 10
            .build();

        when(userRepository.findByEmail("intruder@example.com")).thenReturn(Optional.of(anotherCustomer));
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getCustomerOrderById("intruder@example.com", 1L))
            .isInstanceOf(AccessDeniedException.class);
    }
}
