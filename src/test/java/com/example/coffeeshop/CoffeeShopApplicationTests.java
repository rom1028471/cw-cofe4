package com.example.coffeeshop;

import com.example.coffeeshop.dto.UserRegistrationDto;
import com.example.coffeeshop.model.Category;
import com.example.coffeeshop.model.Product;
import com.example.coffeeshop.model.Role;
import com.example.coffeeshop.model.User;
import com.example.coffeeshop.repository.CategoryRepository;
import com.example.coffeeshop.repository.ProductRepository;
import com.example.coffeeshop.repository.RoleRepository;
import com.example.coffeeshop.repository.UserRepository;
import com.example.coffeeshop.service.CategoryService;
import com.example.coffeeshop.service.ProductService;
import com.example.coffeeshop.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoffeeShopApplicationTests {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private CategoryRepository categoryRepository; // Добавлено для CategoryService теста

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ProductService productService;

    @InjectMocks
    private UserService userService;

    @InjectMocks
    private CategoryService categoryService; // Добавлено для CategoryService теста

    // Тест 1: Проверка получения продукта по ID
    @Test
    void testFindProductById() {
        Product mockProduct = new Product();
        mockProduct.setId(1L);
        mockProduct.setName("Тестовый Кофе");
        mockProduct.setPrice(new BigDecimal("100.00"));

        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));

        Optional<Product> foundProductOpt = productService.findProductById(1L);

        assertTrue(foundProductOpt.isPresent(), "Продукт должен быть найден");
        assertEquals("Тестовый Кофе", foundProductOpt.get().getName(), "Имя продукта должно совпадать");
        verify(productRepository).findById(1L);
    }

    // Тест 2: Проверка получения всех продуктов (когда список пуст)
    @Test
    void testFindAllProducts_whenEmpty() {
        when(productRepository.findAll(any(Specification.class))).thenReturn(Collections.emptyList());

        List<Product> products = productService.findAllProducts();

        assertTrue(products.isEmpty(), "Список продуктов должен быть пустым");
        verify(productRepository).findAll(any(Specification.class));
    }

    // Тест 3: Проверка регистрации нового пользователя
    @Test
    void testRegisterUser() {
        UserRegistrationDto dto = new UserRegistrationDto("newUser", "newuser@example.com", "password123");
        Role userRole = new Role();
        userRole.setName("ROLE_USER");

        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User userToSave = invocation.getArgument(0);
            userToSave.setId(1L); // Имитируем сохранение и присвоение ID
            return userToSave;
        });

        User registeredUser = userService.registerUser(dto.getUsername(), dto.getEmail(), dto.getPassword());

        assertNotNull(registeredUser, "Зарегистрированный пользователь не должен быть null");
        assertEquals("newUser", registeredUser.getUsername(), "Имя пользователя должно совпадать");
        assertEquals("encodedPassword", registeredUser.getPassword(), "Пароль должен быть закодирован");
        assertTrue(registeredUser.getRoles().contains(userRole), "Пользователь должен иметь роль ROLE_USER");

        verify(roleRepository).findByName("ROLE_USER");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
    }

    // Тест 4: Проверка поиска пользователя по имени (когда пользователь не найден)
    @Test
    void testFindByUsername_whenUserNotFound() {
        when(userRepository.findByUsername("nonExistingUser")).thenReturn(Optional.empty());

        Optional<User> userOpt = userService.findByUsername("nonExistingUser");

        assertFalse(userOpt.isPresent(), "Пользователь не должен быть найден");
        verify(userRepository).findByUsername("nonExistingUser");
    }

    // Тест 5: Простой тест модели Category (геттеры/сеттеры)
    @Test
    void testCategoryModel() {
        Category category = new Category();
        category.setId(10L);
        category.setName("Десерты");

        assertEquals(10L, category.getId(), "ID категории должен совпадать");
        assertEquals("Десерты", category.getName(), "Имя категории должно совпадать");
    }

    // Тест 6: Проверка сохранения новой категории
    @Test
    void testSaveCategory() {
        Category newCategory = new Category();
        newCategory.setName("Новая Категория");

        Category savedCategory = new Category();
        savedCategory.setId(1L);
        savedCategory.setName("Новая Категория");

        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        Category result = categoryService.saveCategory(newCategory);

        assertNotNull(result, "Сохраненная категория не должна быть null");
        assertEquals(1L, result.getId(), "ID сохраненной категории должен быть 1L");
        assertEquals("Новая Категория", result.getName(), "Имя категории должно совпадать");
        verify(categoryRepository).save(newCategory);
    }

    // Тест 7: Проверка поиска пользователя по email (когда пользователь существует)
    @Test
    void testFindByEmail_whenUserExists() {
        User mockUser = new User();
        mockUser.setId(2L);
        mockUser.setEmail("test@example.com");
        mockUser.setUsername("testuser");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockUser));

        Optional<User> foundUserOpt = userService.findByEmail("test@example.com");

        assertTrue(foundUserOpt.isPresent(), "Пользователь должен быть найден по email");
        assertEquals("testuser", foundUserOpt.get().getUsername(), "Username найденного пользователя должен совпадать");
        verify(userRepository).findByEmail("test@example.com");
    }
} 