package com.example.coffeeshop.repository;

import com.example.coffeeshop.model.Cart;
import com.example.coffeeshop.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUser(User user);
    Optional<Cart> findByUserId(Long userId);
} 