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

    /**
     * Test creating an order successfully
     */
    @Test
    @DisplayName("Should create order successfully")
    void createOrder_ShouldCreateOrderSuccessfully() {
        when(shoppingCartService.getCart(testUser)).thenReturn(testCart);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        doNothing().when(shoppingCartRepository).flush();
        doNothing().when(orderRepository).flush();

        Long orderId = orderService.createOrder(testUser, testCart, "Test Address", "Credit Card");

        assertNotNull(orderId);
        assertEquals(testOrder.getId(), orderId);
        verify(orderRepository).save(any(Order.class));
        verify(shoppingCartService).getCart(testUser);
        verify(shoppingCartRepository).flush();
        verify(orderRepository).flush();
    }

    /**
     * Test retrieving an order by ID successfully
     */
    @Test
    @DisplayName("Should get order by ID successfully")
    void getOrderById_ShouldReturnOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        Order foundOrder = orderService.getOrderById(1L);

        assertNotNull(foundOrder);
        assertEquals(testOrder.getId(), foundOrder.getId());
        verify(orderRepository).findById(1L);
    }

    /**
     * Test handling order didn't find exception
     */
    @Test
    @DisplayName("Should throw OrderNotFoundException when order not found")
    void getOrderById_ShouldThrowException() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(999L));
        verify(orderRepository).findById(999L);
    }

    /**
     * Test retrieving recent orders for a user
     */
    @Test
    @DisplayName("Should get recent orders for user")
    void getRecentOrdersForUser_ShouldReturnOrders() {
        List<Order> orders = Collections.singletonList(testOrder);
        when(orderRepository.findByUserOrderByOrderDateDesc(eq(testUser), any(PageRequest.class)))
                .thenReturn(orders);

        List<Order> recentOrders = orderService.getRecentOrdersForUser(testUser, 5);

        assertFalse(recentOrders.isEmpty());
        assertEquals(1, recentOrders.size());
        verify(orderRepository).findByUserOrderByOrderDateDesc(eq(testUser), any(PageRequest.class));
    }

    /**
     * Test retrieving all orders for a user
     */
    @Test
    @DisplayName("Should get all orders for user")
    void getAllOrdersForUser_ShouldReturnOrders() {
        List<Order> orders = Collections.singletonList(testOrder);
        when(orderRepository.findByUserOrderByOrderDateDesc(testUser)).thenReturn(orders);

        List<Order> allOrders = orderService.getAllOrdersForUser(testUser);

        assertFalse(allOrders.isEmpty());
        assertEquals(1, allOrders.size());
        verify(orderRepository).findByUserOrderByOrderDateDesc(testUser);
    }

    /**
     * Test retrieving an order by ID and user
     */
    @Test
    @DisplayName("Should get order by ID and user")
    void getOrderByIdAndUser_ShouldReturnOrder() {
        when(orderRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testOrder));

        Order foundOrder = orderService.getOrderByIdAndUser(1L, testUser);

        assertNotNull(foundOrder);
        assertEquals(testOrder.getId(), foundOrder.getId());
        assertEquals(testOrder.getUser(), foundOrder.getUser());
        verify(orderRepository).findByIdAndUser(1L, testUser);
    }

    /**
     * Test retrieving all orders
     */
    @Test
    @DisplayName("Should get all orders")
    void getAllOrders_ShouldReturnAllOrders() {
        List<Order> orders = Collections.singletonList(testOrder);
        when(orderRepository.findAll()).thenReturn(orders);

        List<Order> allOrders = orderService.getAllOrders();

        assertFalse(allOrders.isEmpty());
        assertEquals(1, allOrders.size());
        verify(orderRepository).findAll();
    }

    /**
     * Test updating an order's status
     */
    @Test
    @DisplayName("Should update order status successfully")
    void updateOrderStatus_ShouldUpdateStatus() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        orderService.updateOrderStatus(1L, "SHIPPED");

        assertEquals("SHIPPED", testOrder.getStatus());
        verify(orderRepository).save(testOrder);
    }

    /**
     * Test canceling an order as an admin
     */
    @Test
    @DisplayName("Should cancel order as admin successfully")
    void cancelOrder_AsAdmin_ShouldCancelOrder() {
        testUser.addRole("ROLE_ADMIN");
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        orderService.cancelOrder(1L, testUser);

        assertEquals("CANCELLED", testOrder.getStatus());
        verify(orderRepository).save(testOrder);
    }

    /**
     * Test canceling an order as a user
     */
    @Test
    @DisplayName("Should cancel order as user successfully")
    void cancelOrder_AsUser_ShouldCancelOrder() {
        when(orderRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testOrder));

        orderService.cancelOrder(1L, testUser);

        assertEquals("CANCELLED", testOrder.getStatus());
        verify(orderRepository).save(testOrder);
    }

    /**
     * Test handling an exception when canceling a non-pending order
     */
    @Test
    @DisplayName("Should throw exception when cancelling non-pending order")
    void cancelOrder_NonPendingOrder_ShouldThrowException() {
        testOrder.setStatus("SHIPPED");
        when(orderRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testOrder));

        assertThrows(IllegalStateException.class, () -> orderService.cancelOrder(1L, testUser));
    }

    /**
     * Test deleting an order
     */
    @Test
    @DisplayName("Should delete order successfully")
    void deleteOrder_ShouldDeleteOrder() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(testOrder));

        orderService.deleteOrder(1L);

        verify(orderRepository).delete(testOrder);
    }
}
