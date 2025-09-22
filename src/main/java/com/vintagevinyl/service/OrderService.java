package com.vintagevinyl.service;

import com.vintagevinyl.exception.OrderNotFoundException;
import com.vintagevinyl.model.CartItem;
import com.vintagevinyl.model.Order;
import com.vintagevinyl.model.OrderItem;
import com.vintagevinyl.model.ShoppingCart;
import com.vintagevinyl.model.User;
import com.vintagevinyl.repository.OrderRepository;
import com.vintagevinyl.repository.ShoppingCartRepository;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Service class for managing orders.
 *
 * This service handles the creation, retrieval, updating, and deletion of orders.
 * It also interacts with the shopping cart to create orders and ensure stock availability.
 */
@Service
public class OrderService {

    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private ShoppingCartRepository shoppingCartRepository;

    /**
     * Creates a new order for the specified user and shopping cart.
     *
     * @param user the user placing the order
     * @param cart the shopping cart associated with the order
     * @param shippingAddress the shipping address for the order
     * @param paymentMethod the payment method for the order
     * @return the ID of the created order
     */
    @Transactional
    public Long createOrder(User user, ShoppingCart cart, String shippingAddress, String paymentMethod) {
        System.out.println("Creating order for user: " + user.getUsername());

        // Refresh cart data to ensure it's up-to-date
        cart = shoppingCartService.getCart(user);

        // Ensure the cart is not empty
        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot create order with empty cart");
        }

        // Initialize the order
        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(new Date());
        order.setShippingAddress(shippingAddress);
        order.setPaymentMethod(paymentMethod);
        order.setTotal(cart.getTotal());
        order.setStatus("PENDING");

        // Convert cart items to order items
        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cart.getItems()) {
            com.vintagevinyl.model.Record vinyl = cartItem.getRecord();

            // Check if stock is sufficient
            if (vinyl.getStock() < cartItem.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for record: " + vinyl.getTitle());
            }

            // Deduct stock
            vinyl.setStock(vinyl.getStock() - cartItem.getQuantity());

            // Create an order item
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setRecord(vinyl);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(vinyl.getPrice());
            orderItems.add(orderItem);
        }
        order.setItems(orderItems);

        // Save the order to the database
        System.out.println("Saving order...");
        order = orderRepository.save(order);
        System.out.println("Order saved successfully. Order ID: " + order.getId());

        // Clear the shopping cart
        shoppingCartService.clearCart(user);

        // Ensure all changes are committed
        shoppingCartRepository.flush();
        orderRepository.flush();

        return order.getId();
    }

    /**
     * Retrieves an order by its ID.
     *
     * @param id the ID of the order
     * @return the retrieved order
     * @throws OrderNotFoundException if no order is found with the specified ID
     */
    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
        Hibernate.initialize(order.getItems());
        return order;
    }

    /**
     * Retrieves a limited number of recent orders for a user.
     *
     * @param user the user whose orders are to be retrieved
     * @param limit the maximum number of orders to retrieve
     * @return a list of recent orders
     */
    @Transactional(readOnly = true)
    public List<Order> getRecentOrdersForUser(User user, int limit) {
        return orderRepository.findByUserOrderByOrderDateDesc(user, PageRequest.of(0, limit));
    }

    /**
     * Retrieves all orders for a user.
     *
     * @param user the user whose orders are to be retrieved
     * @return a list of all orders for the user
     */
    @Transactional(readOnly = true)
    public List<Order> getAllOrdersForUser(User user) {
        return orderRepository.findByUserOrderByOrderDateDesc(user);
    }

    /**
     * Retrieves an order by its ID and user.
     *
     * @param id the ID of the order
     * @param user the user to whom the order belongs
     * @return the retrieved order
     * @throws OrderNotFoundException if no order is found with the specified ID and user
     */
    @Transactional(readOnly = true)
    public Order getOrderByIdAndUser(Long id, User user) {
        Order order = orderRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new OrderNotFoundException("Order not found or does not belong to the user"));
        Hibernate.initialize(order.getItems());
        return order;
    }

    /**
     * Retrieves all orders in the system.
     *
     * @return a list of all orders
     */
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    /**
     * Updates the status of an order.
     *
     * @param orderId the ID of the order
     * @param status the new status for the order
     */
    @Transactional
    public void updateOrderStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
        order.setStatus(status);
        orderRepository.save(order);

        // Log the status update
        logger.info("Updated order {} status to {}", orderId, status);
    }

    /**
     * Cancels an order for an admin or user.
     *
     * @param orderId the ID of the order
     * @param user the user attempting to cancel the order
     * @throws IllegalStateException if the order cannot be canceled
     */
    @Transactional
    public void cancelOrder(Long orderId, User user) {
        if (user.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            Order order = getOrderById(orderId);
            if ("PENDING".equals(order.getStatus())) {
                order.setStatus("CANCELLED");
                orderRepository.save(order);
            } else {
                throw new IllegalStateException("Order cannot be cancelled as it is not in PENDING status");
            }
        } else {
            Order order = getOrderByIdAndUser(orderId, user);
            if ("PENDING".equals(order.getStatus())) {
                order.setStatus("CANCELLED");
                orderRepository.save(order);
            } else {
                throw new IllegalStateException("Order cannot be cancelled as it is not in PENDING status");
            }
        }
    }

    /**
     * Deletes an order by its ID.
     *
     * @param orderId the ID of the order to delete
     */
    @Transactional
    public void deleteOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
        orderRepository.delete(order);
    }
}
