package com.example.coffeeshop.controller;

import com.example.coffeeshop.model.Cart;
import com.example.coffeeshop.service.CartService;
import com.example.coffeeshop.service.OrderService;
import com.example.coffeeshop.service.UserService;
import com.example.coffeeshop.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CartController {

    private final CartService cartService;
    private final OrderService orderService;
    private final UserService userService;

    @Autowired
    public CartController(CartService cartService, OrderService orderService, UserService userService) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.userService = userService;
    }

    @GetMapping("/cart")
    public String cartPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login"; // Если пользователь не аутентифицирован, перенаправляем на логин
        }
        String username = userDetails.getUsername();
        Cart cart = cartService.getCartForUser(username);
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        model.addAttribute("cart", cart);
        model.addAttribute("username", username);
        model.addAttribute("currentUser", user);
        return "cart";
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Long productId, 
                            @RequestParam(defaultValue = "1") int quantity, 
                            @AuthenticationPrincipal UserDetails userDetails, 
                            RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        try {
            cartService.addProductToCart(userDetails.getUsername(), productId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Товар добавлен в корзину!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка добавления товара: " + e.getMessage());
        }
        return "redirect:/main"; // Или redirect на страницу товара, или на корзину
    }

    @PostMapping("/cart/update")
    public String updateCartItem(@RequestParam Long productId, 
                                 @RequestParam int quantity, 
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        try {
            cartService.updateProductQuantityInCart(userDetails.getUsername(), productId, quantity);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка обновления корзины: " + e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam Long productId, 
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        try {
            cartService.removeProductFromCart(userDetails.getUsername(), productId);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка удаления из корзины: " + e.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/cart/checkout")
    public String checkout(@AuthenticationPrincipal UserDetails userDetails, RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        try {
            orderService.createOrder(userDetails.getUsername());
            redirectAttributes.addFlashAttribute("orderSuccessMessage", "Заказ успешно оформлен!");
            // Всплывающее окно будет реализовано на фронтенде
        } catch (IllegalStateException e) {
             redirectAttributes.addFlashAttribute("orderErrorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("orderErrorMessage", "Ошибка оформления заказа: " + e.getMessage());
        }
        return "redirect:/profile"; // Перенаправляем в профиль, где будут видны заказы
    }
} 