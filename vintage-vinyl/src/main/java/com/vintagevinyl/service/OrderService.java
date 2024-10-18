package com.vintagevinyl.service;

import com.vintagevinyl.exception.OrderNotFoundException;
import com.vintagevinyl.model.CartItem;
import com.vintagevinyl.model.Order;
import com.vintagevinyl.model.OrderItem;
import com.vintagevinyl.model.ShoppingCart;
import com.vintagevinyl.model.User;
import com.vintagevinyl.repository.OrderRepository;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Transactional
    public Long createOrder(User user, ShoppingCart cart, String shippingAddress, String paymentMethod) {
        System.out.println("Creating order for user: " + user.getUsername());
        cart = shoppingCartService.getCart(user);
        Order order = new Order();
        order.setUser(user);
        order.setOrderDate(new Date());
        order.setShippingAddress(shippingAddress);
        order.setPaymentMethod(paymentMethod);
        order.setTotal(cart.getTotal());
        order.setStatus("PENDING");

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setRecord(cartItem.getRecord());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getRecord().getPrice());
            orderItems.add(orderItem);
        }
        order.setItems(orderItems);

        System.out.println("Saving order...");
        order = orderRepository.save(order);
        System.out.println("Order saved successfully. Order ID: " + order.getId());
        return order.getId();
    }

    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + id));
        Hibernate.initialize(order.getItems());
        return order;
    }

    @Transactional(readOnly = true)
    public List<Order> getRecentOrdersForUser(User user, int limit) {
        return orderRepository.findByUserOrderByOrderDateDesc(user, PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrdersForUser(User user) {
        return orderRepository.findByUserOrderByOrderDateDesc(user);
    }

    @Transactional(readOnly = true)
    public Order getOrderByIdAndUser(Long id, User user) {
        Order order = orderRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new OrderNotFoundException("Order not found or does not belong to the user"));
        Hibernate.initialize(order.getItems());
        return order;
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional
    public void updateOrderStatus(Long orderId, String status) {
        Order order = getOrderById(orderId);
        order.setStatus(status);
        orderRepository.save(order);
    }

    @Transactional
    public void cancelOrder(Long orderId, User user) {
        Order order = getOrderByIdAndUser(orderId, user);
        if ("PENDING".equals(order.getStatus())) {
            order.setStatus("CANCELLED");
            orderRepository.save(order);
        } else {
            throw new IllegalStateException("Order cannot be cancelled as it is not in PENDING status");
        }
    }

    @Transactional
    public void deleteOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));
        orderRepository.delete(order);
    }

}