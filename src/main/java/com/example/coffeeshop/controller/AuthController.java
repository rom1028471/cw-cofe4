package com.example.coffeeshop.controller;

import com.example.coffeeshop.dto.UserRegistrationDto;
import com.example.coffeeshop.model.Product;
import com.example.coffeeshop.service.ProductService;
import com.example.coffeeshop.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class AuthController {
    private final UserService userService;
    private final ProductService productService;

    @Autowired
    public AuthController(UserService userService, ProductService productService) {
        this.userService = userService;
        this.productService = productService;
    }

    @GetMapping("/")
    public String redirectToMain() {
        return "redirect:/main";
    }

    @GetMapping("/main")
    public String mainPage(Model model, @RequestParam(name = "search", required = false) String searchQuery) {
        List<Product> products;
        if (searchQuery != null && !searchQuery.isEmpty()) {
            products = productService.searchProductsByName(searchQuery);
            model.addAttribute("searchQuery", searchQuery);
        } else {
            products = productService.findAllProducts();
        }
        model.addAttribute("products", products);
        return "main";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("userDto", new UserRegistrationDto());
        return "register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("userDto") UserRegistrationDto userDto, Model model) {
        try {
            userService.registerUser(userDto.getUsername(), userDto.getEmail(), userDto.getPassword());
            return "redirect:/login";
        } catch (Exception e) {
            model.addAttribute("error", "Ошибка регистрации: " + e.getMessage());
            model.addAttribute("userDto", userDto);
            return "register";
        }
    }

    @GetMapping("/login")
    public String loginPage(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/main";
        }
        return "login";
    }
} 