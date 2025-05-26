package com.example.coffeeshop.service;

import com.example.coffeeshop.model.*;
import com.example.coffeeshop.repository.OrderRepository;
import com.example.coffeeshop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CartService cartService; // Для очистки корзины после заказа
    private final LoyaltyService loyaltyService; // Добавляем LoyaltyService
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    public OrderService(OrderRepository orderRepository, UserRepository userRepository, CartService cartService, LoyaltyService loyaltyService) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.cartService = cartService;
        this.loyaltyService = loyaltyService; // Сохраняем
    }

    @Transactional
    public Order createOrder(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        
        Cart cart = cartService.getCartForUser(username);
        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot create order from an empty cart.");
        }

        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.NEW); // Используем Enum

        Set<OrderItem> orderItems = cart.getItems().stream()
                .map(cartItem -> {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setProduct(cartItem.getProduct());
                    orderItem.setQuantity(cartItem.getQuantity());
                    orderItem.setPriceAtPurchase(cartItem.getProduct().getPrice()); // Product.price уже BigDecimal
                    orderItem.setOrder(order); 
                    return orderItem;
                })
                .collect(Collectors.toSet());
        
        order.setItems(orderItems);

        BigDecimal subtotal = orderItems.stream()
                .map(item -> item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Применяем скидку пользователя
        BigDecimal discountRate = user.getDiscountRate() != null ? user.getDiscountRate() : BigDecimal.ZERO;
        BigDecimal discountAmount = subtotal.multiply(discountRate);
        BigDecimal totalPrice = subtotal.subtract(discountAmount);

        order.setTotalPrice(totalPrice); // Общая цена с учетом скидки
        // Можно добавить поля для хранения суммы скидки и цены без скидки, если нужно
        // order.setSubtotal(subtotal);
        // order.setDiscountApplied(discountAmount);

        Order savedOrder = orderRepository.save(order);
        cartService.clearCart(username); 
        return savedOrder;
    }

    public List<Order> getOrdersForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
    }
    
    // Методы для админки
    public List<Order> findAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc(); // Сортировка по убыванию даты создания
    }

    public Optional<Order> findOrderById(Long id) {
        return orderRepository.findById(id);
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus status) { // Используем Enum OrderStatus
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        order.setStatus(status);
        Order updatedOrder = orderRepository.save(order);

        // Если заказ выполнен, обновляем статус лояльности пользователя
        if (status == OrderStatus.COMPLETED) {
            logger.info("Order {} COMPLETED. Updating loyalty status for user: {}", orderId, order.getUser().getUsername());
            loyaltyService.updateUserLoyaltyStatus(order.getUser().getUsername());
            logger.info("Loyalty status update initiated for user: {}", order.getUser().getUsername());
        }

        return updatedOrder;
    }
    // TODO: Возможно, понадобится метод для удаления заказа, но обычно заказы не удаляют, а отменяют.
} 