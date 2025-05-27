package com.example.coffeeshop.controller;

import com.example.coffeeshop.dto.UserRegistrationDto;
import com.example.coffeeshop.model.Product;
import com.example.coffeeshop.service.CategoryService;
import com.example.coffeeshop.service.ProductService;
import com.example.coffeeshop.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Controller
public class AuthController {
    private final UserService userService;
    private final ProductService productService;
    private final CategoryService categoryService;

    @Autowired
    public AuthController(UserService userService, ProductService productService, CategoryService categoryService) {
        this.userService = userService;
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping("/")
    public String redirectToMain() {
        return "redirect:/main";
    }

    @GetMapping("/main")
    public String mainPage(Model model,
                           @RequestParam(name = "search", required = false) String searchQuery,
                           @RequestParam(name = "categoryId", required = false) Long categoryId,
                           @RequestParam(name = "minPrice", required = false) BigDecimal minPrice,
                           @RequestParam(name = "maxPrice", required = false) BigDecimal maxPrice,
                           @RequestParam(name = "page", defaultValue = "0") int page,
                           @RequestParam(name = "size", defaultValue = "9") int size,
                           @RequestParam(name = "sortField", defaultValue = "name") String sortField,
                           @RequestParam(name = "sortDir", defaultValue = "ASC") String sortDir) {

        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Page<Product> productPage = productService.findProducts(searchQuery, categoryId, minPrice, maxPrice, pageable);

        model.addAttribute("productsPage", productPage);
        model.addAttribute("categories", categoryService.findAllCategories());
        model.addAttribute("searchQuery", searchQuery);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDir);
        model.addAttribute("reverseSortDir", "ASC".equalsIgnoreCase(sortDir) ? "DESC" : "ASC");

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