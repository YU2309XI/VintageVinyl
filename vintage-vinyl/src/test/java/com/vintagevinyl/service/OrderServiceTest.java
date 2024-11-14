package com.vintagevinyl.service;

import com.vintagevinyl.exception.OrderNotFoundException;
import com.vintagevinyl.model.*;
import com.vintagevinyl.model.Record;
import com.vintagevinyl.repository.OrderRepository;
import com.vintagevinyl.repository.ShoppingCartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ShoppingCartService shoppingCartService;

    @Mock
    private ShoppingCartRepository shoppingCartRepository;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private ShoppingCart testCart;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testUser");
        testUser.setEmail("test@example.com");
        testUser.addRole("ROLE_USER");

        Record testRecord = new Record();
        testRecord.setId(1L);
        testRecord.setTitle("Test Record");
        testRecord.setPrice(new BigDecimal("29.99"));
        testRecord.setStock(10); // Adding sufficient stock for the test

        CartItem testCartItem = new CartItem();
        testCartItem.setRecord(testRecord);
        testCartItem.setQuantity(2);

        testCart = new ShoppingCart();
        testCart.setUser(testUser);
        testCart.setItems(new ArrayList<>(Collections.singletonList(testCartItem)));
        testCartItem.setCart(testCart);

        OrderItem testOrderItem = new OrderItem();
        testOrderItem.setRecord(testRecord);
        testOrderItem.setQuantity(2);
        testOrderItem.setPrice(new BigDecimal("29.99"));

        testOrder = new Order();
        testOrder.setId(1L);
        testOrder.setUser(testUser);
        testOrder.setOrderDate(new Date());
        testOrder.setTotal(new BigDecimal("59.98"));
        testOrder.setShippingAddress("Test Address");
        testOrder.setPaymentMethod("Credit Card");
        testOrder.setStatus("PENDING");
        testOrder.setItems(Collections.singletonList(testOrderItem));
    }

    @Test
    @DisplayName("Should create order successfully")
    void createOrder_ShouldCreateOrderSuccessfully() {
        // Given
        when(shoppingCartService.getCart(testUser)).thenReturn(testCart);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doNothing().when(shoppingCartRepository).flush();
        doNothing().when(orderRepository).flush();

        // When
        Long orderId = orderService.createOrder(testUser, testCart, "Test Address", "Credit Card");

        // Then
        assertNotNull(orderId);
        assertEquals(testOrder.getId(), orderId);
        verify(orderRepository).save(any(Order.class));
        verify(shoppingCartService).getCart(testUser);
        verify(shoppingCartRepository).flush();
        verify(orderRepository).flush();
    }

    @Test
    @DisplayName("Should get order by ID successfully")
    void getOrderById_ShouldReturnOrder() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When
        Order foundOrder = orderService.getOrderById(1L);

        // Then
        assertNotNull(foundOrder);
        assertEquals(testOrder.getId(), foundOrder.getId());
        verify(orderRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw OrderNotFoundException when order not found")
    void getOrderById_ShouldThrowException() {
        // Given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(999L));
        verify(orderRepository).findById(999L);
    }

    @Test
    @DisplayName("Should get recent orders for user")
    void getRecentOrdersForUser_ShouldReturnOrders() {
        // Given
        List<Order> orders = Collections.singletonList(testOrder);
        when(orderRepository.findByUserOrderByOrderDateDesc(eq(testUser), any(PageRequest.class)))
                .thenReturn(orders);

        // When
        List<Order> recentOrders = orderService.getRecentOrdersForUser(testUser, 5);

        // Then
        assertFalse(recentOrders.isEmpty());
        assertEquals(1, recentOrders.size());
        verify(orderRepository).findByUserOrderByOrderDateDesc(eq(testUser), any(PageRequest.class));
    }

    @Test
    @DisplayName("Should get all orders for user")
    void getAllOrdersForUser_ShouldReturnOrders() {
        // Given
        List<Order> orders = Collections.singletonList(testOrder);
        when(orderRepository.findByUserOrderByOrderDateDesc(testUser)).thenReturn(orders);

        // When
        List<Order> allOrders = orderService.getAllOrdersForUser(testUser);

        // Then
        assertFalse(allOrders.isEmpty());
        assertEquals(1, allOrders.size());
        verify(orderRepository).findByUserOrderByOrderDateDesc(testUser);
    }

    @Test
    @DisplayName("Should get order by ID and user")
    void getOrderByIdAndUser_ShouldReturnOrder() {
        // Given
        when(orderRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testOrder));

        // When
        Order foundOrder = orderService.getOrderByIdAndUser(1L, testUser);

        // Then
        assertNotNull(foundOrder);
        assertEquals(testOrder.getId(), foundOrder.getId());
        assertEquals(testOrder.getUser(), foundOrder.getUser());
        verify(orderRepository).findByIdAndUser(1L, testUser);
    }

    @Test
    @DisplayName("Should get all orders")
    void getAllOrders_ShouldReturnAllOrders() {
        // Given
        List<Order> orders = Collections.singletonList(testOrder);
        when(orderRepository.findAll()).thenReturn(orders);

        // When
        List<Order> allOrders = orderService.getAllOrders();

        // Then
        assertFalse(allOrders.isEmpty());
        assertEquals(1, allOrders.size());
        verify(orderRepository).findAll();
    }

    @Test
    @DisplayName("Should update order status successfully")
    void updateOrderStatus_ShouldUpdateStatus() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When
        orderService.updateOrderStatus(1L, "SHIPPED");

        // Then
        assertEquals("SHIPPED", testOrder.getStatus());
        verify(orderRepository).save(testOrder);
    }

    @Test
    @DisplayName("Should cancel order as admin successfully")
    void cancelOrder_AsAdmin_ShouldCancelOrder() {
        // Given
        testUser.addRole("ROLE_ADMIN");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When
        orderService.cancelOrder(1L, testUser);

        // Then
        assertEquals("CANCELLED", testOrder.getStatus());
        verify(orderRepository).save(testOrder);
    }

    @Test
    @DisplayName("Should cancel order as user successfully")
    void cancelOrder_AsUser_ShouldCancelOrder() {
        // Given
        when(orderRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testOrder));

        // When
        orderService.cancelOrder(1L, testUser);

        // Then
        assertEquals("CANCELLED", testOrder.getStatus());
        verify(orderRepository).save(testOrder);
    }

    @Test
    @DisplayName("Should throw exception when cancelling non-pending order")
    void cancelOrder_NonPendingOrder_ShouldThrowException() {
        // Given
        testOrder.setStatus("SHIPPED");
        when(orderRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testOrder));

        // When & Then
        assertThrows(IllegalStateException.class, () -> orderService.cancelOrder(1L, testUser));
    }

    @Test
    @DisplayName("Should delete order successfully")
    void deleteOrder_ShouldDeleteOrder() {
        // Given
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        // When
        orderService.deleteOrder(1L);

        // Then
        verify(orderRepository).delete(testOrder);
    }
}