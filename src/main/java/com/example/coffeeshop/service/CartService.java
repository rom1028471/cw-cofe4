package com.example.coffeeshop.service;

import com.example.coffeeshop.model.*;
import com.example.coffeeshop.repository.CartRepository;
import com.example.coffeeshop.repository.ProductRepository;
import com.example.coffeeshop.repository.UserRepository; // Понадобится для получения пользователя
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException; // Для обработки отсутствия пользователя
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Autowired
    public CartService(CartRepository cartRepository, ProductRepository productRepository, UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Cart getCartForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return cartRepository.findByUser(user).orElseGet(() -> {
            Cart newCart = new Cart();
            newCart.setUser(user);
            return cartRepository.save(newCart);
        });
    }

    @Transactional
    public void addProductToCart(String username, Long productId, int quantity) {
        Cart cart = getCartForUser(username);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
        } else {
            CartItem newItem = new CartItem();
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            // newItem.setCart(cart); // Если бы была двунаправленная связь в CartItem
            cart.addItem(newItem); // Используем вспомогательный метод в Cart
        }
        cartRepository.save(cart);
    }

    @Transactional
    public void updateProductQuantityInCart(String username, Long productId, int quantity) {
        Cart cart = getCartForUser(username);
        cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .ifPresent(item -> {
                    if (quantity > 0) {
                        item.setQuantity(quantity);
                    } else {
                        cart.removeItem(item); // Удаляем, если количество 0 или меньше
                    }
                });
        cartRepository.save(cart);
    }

    @Transactional
    public void removeProductFromCart(String username, Long productId) {
        Cart cart = getCartForUser(username);
        cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        cartRepository.save(cart);
    }

    @Transactional
    public void clearCart(String username) {
        Cart cart = getCartForUser(username);
        cart.getItems().clear();
        cartRepository.save(cart);
    }
} 