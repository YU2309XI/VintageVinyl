package com.vintagevinyl.repository;

import com.vintagevinyl.model.Order;
import com.vintagevinyl.model.OrderItem;
import com.vintagevinyl.model.Record;
import com.vintagevinyl.model.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    private User testUser;
    private Record testRecord;
    private Order testOrder;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();

        // Create test user
        testUser = new User();
        testUser.setUsername("testUser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser.setEnabled(true);
        testUser.setAccountNonLocked(true);
        testUser.setCreatedAt(LocalDateTime.now());
        entityManager.persist(testUser);

        // Create test record
        testRecord = new Record();
        testRecord.setTitle("Test Album");
        testRecord.setArtist("Test Artist");
        testRecord.setPrice(BigDecimal.valueOf(29.99));
        testRecord.setStock(10);
        entityManager.persist(testRecord);

        // Create test order
        testOrder = createTestOrder(testUser, testRecord);
        entityManager.flush();
    }

    private Order createTestOrder(User user, Record record) {
        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(new Date());
        order.setTotal(BigDecimal.valueOf(29.99));
        order.setShippingAddress("123 Test St");
        order.setPaymentMethod("Credit Card");
        order.setStatus("PENDING");

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setRecord(record);
        orderItem.setQuantity(1);
        orderItem.setPrice(record.getPrice());

        List<OrderItem> items = new ArrayList<>();
        items.add(orderItem);
        order.setItems(items);

        return order;
    }

    @Test
    @DisplayName("Should save and retrieve an order")
    void shouldSaveAndRetrieveOrder() {
        // Arrange & Act
        Order savedOrder = orderRepository.save(testOrder);
        Optional<Order> foundOrder = orderRepository.findById(savedOrder.getId());

        // Assert
        assertThat(foundOrder)
                .isPresent()
                .hasValueSatisfying(order -> {
                    assertThat(order.getUser()).isEqualTo(testUser);
                    assertThat(order.getTotal()).isEqualTo(BigDecimal.valueOf(29.99));
                    assertThat(order.getStatus()).isEqualTo("PENDING");
                });
    }

    @Test
    @DisplayName("Should find orders by user")
    void shouldFindOrdersByUser() {
        // Arrange
        Order savedOrder = orderRepository.save(testOrder);

        // Act
        List<Order> userOrders = orderRepository.findByUser(testUser);

        // Assert
        assertThat(userOrders)
                .hasSize(1)
                .element(0)
                .satisfies(order -> {
                    assertThat(order.getId()).isEqualTo(savedOrder.getId());
                    assertThat(order.getUser()).isEqualTo(testUser);
                });
    }

    @Test
    @DisplayName("Should find orders by user ordered by date")
    void shouldFindOrdersByUserOrderedByDate() {
        // Arrange
        Order order1 = createTestOrder(testUser, testRecord);
        order1.setOrderDate(new Date(System.currentTimeMillis() - 10000)); // Earlier order

        Order order2 = createTestOrder(testUser, testRecord);
        order2.setOrderDate(new Date()); // Recent order

        orderRepository.saveAll(List.of(order1, order2));

        // Act
        List<Order> orderedOrders = orderRepository.findByUserOrderByOrderDateDesc(testUser);

        // Assert
        assertThat(orderedOrders)
                .hasSize(2)
                .extracting(Order::getOrderDate)
                .isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    @DisplayName("Should find orders with pagination")
    void shouldFindOrdersWithPagination() {
        // Arrange
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Order order = createTestOrder(testUser, testRecord);
            order.setOrderDate(new Date(System.currentTimeMillis() + i * 1000));
            orders.add(order);
        }
        orderRepository.saveAll(orders);

        // Act
        List<Order> pageOne = orderRepository.findByUserOrderByOrderDateDesc(
                testUser,
                PageRequest.of(0, 2)
        );
        List<Order> pageTwo = orderRepository.findByUserOrderByOrderDateDesc(
                testUser,
                PageRequest.of(1, 2)
        );

        // Assert
        assertThat(pageOne).hasSize(2);
        assertThat(pageTwo).hasSize(2);
        assertThat(pageOne)
                .extracting(Order::getOrderDate)
                .isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    @DisplayName("Should find order by id and user")
    void shouldFindOrderByIdAndUser() {
        // Arrange
        Order savedOrder = orderRepository.save(testOrder);

        // Act
        Optional<Order> foundOrder = orderRepository.findByIdAndUser(
                savedOrder.getId(),
                testUser
        );

        // Assert
        assertThat(foundOrder).isPresent();
        assertThat(foundOrder.get().getId()).isEqualTo(savedOrder.getId());
        assertThat(foundOrder.get().getUser()).isEqualTo(testUser);
    }

    @Test
    @DisplayName("Should find order with items")
    void shouldFindOrderWithItems() {
        // Arrange
        Order savedOrder = orderRepository.save(testOrder);

        // Act
        Optional<Order> foundOrder = orderRepository.findByIdWithItems(savedOrder.getId());

        // Assert
        assertThat(foundOrder)
                .isPresent()
                .hasValueSatisfying(order -> {
                    assertThat(order.getItems()).hasSize(1);
                    OrderItem item = order.getItems().get(0);
                    assertThat(item.getRecord()).isEqualTo(testRecord);
                    assertThat(item.getQuantity()).isEqualTo(1);
                });
    }

    @Test
    @DisplayName("Should handle non-existent order")
    void shouldHandleNonExistentOrder() {
        // Act & Assert
        assertThat(orderRepository.findById(999L)).isEmpty();
        assertThat(orderRepository.findByIdAndUser(999L, testUser)).isEmpty();
        assertThat(orderRepository.findByIdWithItems(999L)).isEmpty();
    }

    @Test
    @DisplayName("Should find multiple orders for same user")
    void shouldFindMultipleOrdersForSameUser() {
        // Arrange
        List<Order> orders = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Order order = createTestOrder(testUser, testRecord);
            order.setOrderDate(new Date(System.currentTimeMillis() + i * 1000));
            orders.add(order);
        }
        orderRepository.saveAll(orders);

        // Act
        List<Order> userOrders = orderRepository.findByUser(testUser);

        // Assert
        assertThat(userOrders)
                .hasSize(3)
                .extracting(Order::getUser)
                .containsOnly(testUser);
    }
}