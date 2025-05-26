package com.example.coffeeshop;

import com.example.coffeeshop.model.Category;
import com.example.coffeeshop.model.Product;
import com.example.coffeeshop.model.Role;
import com.example.coffeeshop.model.User;
import com.example.coffeeshop.repository.CategoryRepository;
import com.example.coffeeshop.repository.ProductRepository;
import com.example.coffeeshop.repository.RoleRepository;
import com.example.coffeeshop.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SpringBootApplication
public class CoffeeshopApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoffeeshopApplication.class, args);
    }

    @Bean
    @Transactional
    public CommandLineRunner initData(RoleRepository roleRepository, 
                                      UserRepository userRepository,
                                      CategoryRepository categoryRepository,
                                      ProductRepository productRepository,
                                      PasswordEncoder passwordEncoder) {
        return args -> {
            System.out.println("---------- INITIALIZING DATA ----------");

            Role userRole = roleRepository.findByName("ROLE_USER").orElseGet(() -> {
                Role newUserRole = new Role();
                newUserRole.setName("ROLE_USER");
                return roleRepository.save(newUserRole);
            });

            Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> {
                Role newAdminRole = new Role();
                newAdminRole.setName("ROLE_ADMIN");
                return roleRepository.save(newAdminRole);
            });

            System.out.println("Available roles after init:");
            roleRepository.findAll().forEach(role -> System.out.println("Role ID: " + role.getId() + ", Name: " + role.getName()));

            if (userRepository.findByUsername("admin").isEmpty()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setEmail("admin@coffee.com");
                admin.setPassword(passwordEncoder.encode("admin"));
                admin.setFirstName("Администратор");
                admin.setLastName("Главный");
                Set<Role> adminRoles = new HashSet<>();
                adminRoles.add(adminRole);
                admin.setRoles(adminRoles);
                userRepository.save(admin);
                System.out.println("Admin user created.");
            }

            if (userRepository.findByUsername("user").isEmpty()) {
                User regularUser = new User();
                regularUser.setUsername("user");
                regularUser.setEmail("user@coffee.com");
                regularUser.setPassword(passwordEncoder.encode("password"));
                regularUser.setFirstName("Иван");
                regularUser.setLastName("Иванов");
                Set<Role> userRoles = new HashSet<>();
                userRoles.add(userRole);
                regularUser.setRoles(userRoles);
                userRepository.save(regularUser);
                System.out.println("Regular user created.");
            }

            Category coffeeCategory = categoryRepository.findByName("Кофе").orElseGet(() -> {
                Category category = new Category();
                category.setName("Кофе");
                return categoryRepository.save(category);
            });
            System.out.println("Coffee category ID: " + coffeeCategory.getId());

            Category desertCategory = categoryRepository.findByName("Десерты").orElseGet(() -> {
                Category category = new Category();
                category.setName("Десерты");
                return categoryRepository.save(category);
            });
            System.out.println("Desert category ID: " + desertCategory.getId());

            if (productRepository.findByNameContainingIgnoreCase("Эспрессо").isEmpty()) {
                Product espresso = new Product();
                espresso.setName("Эспрессо");
                espresso.setDescription("Классический эспрессо");
                espresso.setPrice(new BigDecimal("120.00"));
                espresso.setCategory(coffeeCategory);
                espresso.setAvailable(true);
                espresso.setImageName("placeholder.png");
                productRepository.save(espresso);
                System.out.println("Espresso product created.");
            }

            if (productRepository.findByNameContainingIgnoreCase("Капучино").isEmpty()) {
                Product cappuccino = new Product();
                cappuccino.setName("Капучино");
                cappuccino.setDescription("Кофе с молоком");
                cappuccino.setPrice(new BigDecimal("150.00"));
                cappuccino.setCategory(coffeeCategory);
                cappuccino.setAvailable(true);
                cappuccino.setImageName("placeholder.png");
                productRepository.save(cappuccino);
                System.out.println("Cappuccino product created.");
            }

            if (productRepository.findByNameContainingIgnoreCase("Чизкейк").isEmpty()) {
                Product cheesecake = new Product();
                cheesecake.setName("Чизкейк");
                cheesecake.setDescription("Вкусный чизкейк");
                cheesecake.setPrice(new BigDecimal("200.00"));
                cheesecake.setCategory(desertCategory);
                cheesecake.setAvailable(true);
                cheesecake.setImageName("placeholder.png");
                productRepository.save(cheesecake);
                System.out.println("Cheesecake product created.");
            }

            // Дополнительные товары
            if (productRepository.findByNameContainingIgnoreCase("Латте").isEmpty()) {
                Product latte = new Product();
                latte.setName("Латте");
                latte.setDescription("Нежный кофе с большим количеством молока");
                latte.setPrice(new BigDecimal("180.00"));
                latte.setCategory(coffeeCategory);
                latte.setAvailable(true);
                latte.setImageName("placeholder.png");
                productRepository.save(latte);
                System.out.println("Latte product created.");
            }

            if (productRepository.findByNameContainingIgnoreCase("Американо").isEmpty()) {
                Product americano = new Product();
                americano.setName("Американо");
                americano.setDescription("Эспрессо с добавлением горячей воды");
                americano.setPrice(new BigDecimal("130.00"));
                americano.setCategory(coffeeCategory);
                americano.setAvailable(true);
                americano.setImageName("placeholder.png");
                productRepository.save(americano);
                System.out.println("Americano product created.");
            }

            if (productRepository.findByNameContainingIgnoreCase("Тирамису").isEmpty()) {
                Product tiramisu = new Product();
                tiramisu.setName("Тирамису");
                tiramisu.setDescription("Классический итальянский десерт");
                tiramisu.setPrice(new BigDecimal("250.00"));
                tiramisu.setCategory(desertCategory);
                tiramisu.setAvailable(true);
                tiramisu.setImageName("placeholder.png");
                productRepository.save(tiramisu);
                System.out.println("Tiramisu product created.");
            }

            if (productRepository.findByNameContainingIgnoreCase("Эклер").isEmpty()) {
                Product eclair = new Product();
                eclair.setName("Эклер");
                eclair.setDescription("Заварное пирожное с кремом");
                eclair.setPrice(new BigDecimal("100.00"));
                eclair.setCategory(desertCategory);
                eclair.setAvailable(true);
                eclair.setImageName("placeholder.png");
                productRepository.save(eclair);
                System.out.println("Eclair product created.");
            }

            System.out.println("-------------------------------------");
        };
    }
} 