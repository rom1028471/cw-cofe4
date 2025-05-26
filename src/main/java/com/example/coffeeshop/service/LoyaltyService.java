package com.example.coffeeshop.service;

import com.example.coffeeshop.model.Order;
import com.example.coffeeshop.model.User;
import com.example.coffeeshop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.List;

@Service
public class LoyaltyService {

    private final UserRepository userRepository;
    private final OrderService orderService;
    private static final Logger logger = LoggerFactory.getLogger(LoyaltyService.class);

    // Уровни лояльности и скидки
    private static final BigDecimal BRONZE_THRESHOLD = new BigDecimal("1000");
    private static final BigDecimal SILVER_THRESHOLD = new BigDecimal("5000");

    private static final BigDecimal BRONZE_DISCOUNT = new BigDecimal("0.05"); // 5%
    private static final BigDecimal SILVER_DISCOUNT = new BigDecimal("0.10"); // 10%

    private static final String STATUS_NEWBIE = "Новичок";
    private static final String STATUS_BRONZE = "Кофеман";
    private static final String STATUS_SILVER = "Кофе-мастер";

    @Autowired
    public LoyaltyService(UserRepository userRepository, @Lazy OrderService orderService) {
        this.userRepository = userRepository;
        this.orderService = orderService;
    }

    @Transactional
    public void updateUserLoyaltyStatus(String username) {
        logger.info("Attempting to update loyalty status for user: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    logger.error("User not found when updating loyalty status: {}", username);
                    return new RuntimeException("User not found: " + username);
                });

        List<Order> userOrders = orderService.getOrdersForUser(username);
        BigDecimal totalSpent = userOrders.stream()
                .filter(order -> {
                    boolean isCompleted = order.getStatus() == com.example.coffeeshop.model.OrderStatus.COMPLETED;
                    if (!isCompleted) {
                        logger.debug("Order ID {} for user {} is not COMPLETED, current status: {}. Skipping for loyalty calculation.", order.getId(), username, order.getStatus());
                    }
                    return isCompleted;
                })
                .map(Order::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        logger.info("Total spent by user {}: {} on COMPLETED orders.", username, totalSpent);

        String oldStatus = user.getLoyaltyStatus();
        BigDecimal oldDiscount = user.getDiscountRate();

        String newStatus = STATUS_NEWBIE;
        BigDecimal newDiscount = BigDecimal.ZERO;

        if (totalSpent.compareTo(SILVER_THRESHOLD) >= 0) {
            newStatus = STATUS_SILVER;
            newDiscount = SILVER_DISCOUNT;
        } else if (totalSpent.compareTo(BRONZE_THRESHOLD) >= 0) {
            newStatus = STATUS_BRONZE;
            newDiscount = BRONZE_DISCOUNT;
        }

        user.setLoyaltyStatus(newStatus);
        user.setDiscountRate(newDiscount);
        userRepository.save(user);

        String oldDiscountPercent = (oldDiscount != null) ? oldDiscount.multiply(BigDecimal.valueOf(100)).toPlainString() + "%" : "N/A";
        String newDiscountPercent = newDiscount.multiply(BigDecimal.valueOf(100)).toPlainString() + "%";

        logger.info("User {} loyalty status updated from {} ({} discount) to {} ({} discount). Total spent: {}", 
                    username, oldStatus, oldDiscountPercent, newStatus, newDiscountPercent, totalSpent);
    }

    // Метод для получения информации о следующем уровне (для отображения в профиле)
    public LoyaltyProgression getLoyaltyProgression(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        List<Order> userOrders = orderService.getOrdersForUser(username);
        BigDecimal totalSpent = userOrders.stream()
            .filter(order -> order.getStatus() == com.example.coffeeshop.model.OrderStatus.COMPLETED)
            .map(Order::getTotalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        String currentStatus = user.getLoyaltyStatus();
        BigDecimal currentDiscount = user.getDiscountRate();
        String nextStatus = null;
        BigDecimal amountNeededForNextStatus = null;
        BigDecimal nextThreshold = null;

        if (STATUS_NEWBIE.equals(currentStatus)) {
            nextStatus = STATUS_BRONZE;
            nextThreshold = BRONZE_THRESHOLD;
            amountNeededForNextStatus = BRONZE_THRESHOLD.subtract(totalSpent);
        } else if (STATUS_BRONZE.equals(currentStatus)) {
            nextStatus = STATUS_SILVER;
            nextThreshold = SILVER_THRESHOLD;
            amountNeededForNextStatus = SILVER_THRESHOLD.subtract(totalSpent);
        }
        // Если уже максимальный статус, то nextStatus и amountNeededForNextStatus остаются null

        return new LoyaltyProgression(currentStatus, currentDiscount, totalSpent, nextStatus, amountNeededForNextStatus, nextThreshold);
    }

    // Вспомогательный класс для LoyaltyProgression
    public static class LoyaltyProgression {
        public final String currentStatus;
        public final BigDecimal currentDiscount;
        public final BigDecimal totalSpent;
        public final String nextStatus;
        public final BigDecimal amountNeededForNextStatus;
        public final BigDecimal nextThreshold; // Общая сумма для достижения следующего статуса


        public LoyaltyProgression(String currentStatus, BigDecimal currentDiscount, BigDecimal totalSpent, String nextStatus, BigDecimal amountNeededForNextStatus, BigDecimal nextThreshold) {
            this.currentStatus = currentStatus;
            this.currentDiscount = currentDiscount;
            this.totalSpent = totalSpent;
            this.nextStatus = nextStatus;
            this.amountNeededForNextStatus = amountNeededForNextStatus;
            this.nextThreshold = nextThreshold;
        }
    }
} 