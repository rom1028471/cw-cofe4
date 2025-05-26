package com.example.coffeeshop.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "carts")
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "cart_id") // Внешний ключ в cart_items будет ссылаться на эту корзину
    private Set<CartItem> items = new HashSet<>();

    // transient поле для общей суммы, чтобы не хранить в БД, а вычислять на лету
    @Transient
    public BigDecimal getTotalPrice() {
        return items.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Геттеры и сеттеры
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Set<CartItem> getItems() {
        return items;
    }

    public void setItems(Set<CartItem> items) {
        this.items = items;
    }

    // Вспомогательные методы
    public void addItem(CartItem item) {
        this.items.add(item);
        // Если CartItem не имеет ссылки на Cart, и это нужно, установить здесь
    }

    public void removeItem(CartItem item) {
        this.items.remove(item);
    }
} 