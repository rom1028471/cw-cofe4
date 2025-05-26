package com.example.coffeeshop.controller;

import com.example.coffeeshop.model.Category;
import com.example.coffeeshop.model.Product;
import com.example.coffeeshop.model.User;
import com.example.coffeeshop.model.Order;
import com.example.coffeeshop.model.OrderStatus;
import com.example.coffeeshop.service.CategoryService;
import com.example.coffeeshop.service.ProductService;
import com.example.coffeeshop.service.UserService;
import com.example.coffeeshop.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.UUID;
import org.springframework.util.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final UserService userService;
    private final OrderService orderService;
    private final Path productImageUploadPath;

    @Autowired
    public AdminController(ProductService productService, CategoryService categoryService, UserService userService, OrderService orderService, @Value("${file.upload.product-image-path:src/main/resources/static/media/products/}") String productImageUploadPathStr) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.userService = userService;
        this.orderService = orderService;
        this.productImageUploadPath = Paths.get(productImageUploadPathStr).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.productImageUploadPath);
        } catch (IOException e) {
            System.err.println("Could not create upload directory: " + this.productImageUploadPath);
        }
    }

    @GetMapping
    public String adminPage(Model model) {
        return "admin/admin_home";
    }

    // ----- Управление Товарами (Products) -----
    @GetMapping("/products")
    public String listProducts(Model model,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "10") int size,
                               @RequestParam(defaultValue = "id,asc") String[] sort,
                               @RequestParam(required = false) Long categoryId) {

        String sortField = sort[0];
        Sort.Direction sortDirection = (sort.length > 1 && sort[1].equalsIgnoreCase("desc")) ? 
                                        Sort.Direction.DESC : Sort.Direction.ASC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));
        Page<Product> productPage;

        if (categoryId != null && categoryId > 0) {
            productPage = productService.findProductsByCategory(categoryId, pageable);
        } else {
            productPage = productService.findAllProducts(pageable);
        }

        model.addAttribute("productPage", productPage);
        model.addAttribute("categories", categoryService.findAllCategories());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("sortField", sortField);
        model.addAttribute("sortDir", sortDirection.name());
        model.addAttribute("reverseSortDir", sortDirection == Sort.Direction.ASC ? "desc" : "asc");

        return "admin/products_list";
    }

    @GetMapping("/products/add")
    public String showAddProductForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryService.findAllCategories());
        return "admin/product_form";
    }

    @PostMapping("/products/save")
    public String saveProduct(@ModelAttribute("product") Product product,
                              @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                              BindingResult result,
                              RedirectAttributes redirectAttributes, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.findAllCategories());
            return "admin/product_form";
        }

        String oldImageName = product.getImageName();

        if (imageFile != null && !imageFile.isEmpty()) {
            if (imageFile.getSize() > 5 * 1024 * 1024) {
                model.addAttribute("categories", categoryService.findAllCategories());
                model.addAttribute("imageError", "Файл слишком большой (макс. 5MB)");
                return "admin/product_form";
            }
            String originalFilename = StringUtils.cleanPath(imageFile.getOriginalFilename());
            String fileExtension = "";
            if (originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            if (!fileExtension.toLowerCase().matches("\\.(jpg|jpeg|png|gif)")) {
                 model.addAttribute("categories", categoryService.findAllCategories());
                 model.addAttribute("imageError", "Недопустимый тип файла. Разрешены: jpg, jpeg, png, gif.");
                 return "admin/product_form";
            }

            String newImageName = UUID.randomUUID().toString() + fileExtension;
            try {
                Path targetLocation = this.productImageUploadPath.resolve(newImageName);
                Files.copy(imageFile.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
                product.setImageName(newImageName);

                if (product.getId() != null && oldImageName != null && !oldImageName.equals(newImageName)) {
                    try {
                        Path oldFilePath = this.productImageUploadPath.resolve(oldImageName);
                        Files.deleteIfExists(oldFilePath);
                    } catch (IOException e) {
                        System.err.println("Could not delete old image: " + oldImageName + " Error: " + e.getMessage());
                    }
                }
            } catch (IOException ex) {
                model.addAttribute("categories", categoryService.findAllCategories());
                model.addAttribute("imageError", "Не удалось сохранить файл изображения: " + ex.getMessage());
                product.setImageName(oldImageName);
                return "admin/product_form";
            }
        } else if (product.getId() != null) {
            // Если файл не загружен при редактировании, оставляем старое изображение
            // product.setImageName(старое значение уже в объекте product из формы)
        } else {
            // Новый продукт без изображения
            product.setImageName(null);
        }

        productService.saveProduct(product);
        redirectAttributes.addFlashAttribute("successMessage", "Товар '" + product.getName() + "' успешно сохранен!");
        return "redirect:/admin/products";
    }

    @GetMapping("/products/edit/{id}")
    public String showEditProductForm(@PathVariable("id") Long id, Model model, RedirectAttributes redirectAttributes) {
        Product product = productService.findProductById(id).orElse(null);
        if (product == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Товар с ID " + id + " не найден.");
            return "redirect:/admin/products";
        }
        model.addAttribute("product", product);
        model.addAttribute("categories", categoryService.findAllCategories());
        return "admin/product_form";
    }

    @GetMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Product product = productService.findProductById(id).orElse(null);
            if (product == null) {
                 redirectAttributes.addFlashAttribute("errorMessage", "Товар с ID " + id + " не найден для удаления.");
                 return "redirect:/admin/products";
            }
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("successMessage", "Товар '" + product.getName() + "' успешно удален!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка удаления товара: " + e.getMessage());
        }
        return "redirect:/admin/products";
    }

    // ----- Управление Категориями (Categories) -----
    @GetMapping("/categories")
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryService.findAllCategories());
        return "admin/categories_list";
    }

    @GetMapping("/categories/add")
    public String showAddCategoryForm(Model model) {
        model.addAttribute("category", new Category());
        return "admin/category_form";
    }

    @PostMapping("/categories/save")
    public String saveCategory(@ModelAttribute("category") Category category,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {
         if (result.hasErrors()) {
            return "admin/category_form";
        }
        categoryService.saveCategory(category);
        redirectAttributes.addFlashAttribute("successMessage", "Категория '" + category.getName() + "' успешно сохранена!");
        return "redirect:/admin/categories";
    }

    @GetMapping("/categories/edit/{id}")
    public String showEditCategoryForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        Category category = categoryService.findCategoryById(id).orElse(null);
        if (category == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Категория с ID " + id + " не найдена!");
            return "redirect:/admin/categories";
        }
        model.addAttribute("category", category);
        return "admin/category_form";
    }

    @GetMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Category category = categoryService.findCategoryById(id).orElse(null);
            if (category == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "Категория с ID " + id + " не найдена для удаления.");
                return "redirect:/admin/categories";
            }
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("successMessage", "Категория '" + category.getName() + "' успешно удалена!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка удаления категории: " + e.getMessage() + ". Возможно, есть связанные товары.");
        }
        return "redirect:/admin/categories";
    }

    // ----- Управление Пользователями (Users) -----
    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("users", userService.findAllUsers());
        return "admin/users_list";
    }

    @GetMapping("/users/edit/{id}")
    public String showEditUserRolesForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        User user = userService.findUserById(id).orElse(null);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Пользователь с ID " + id + " не найден!");
            return "redirect:/admin/users";
        }
        model.addAttribute("user", user);
        model.addAttribute("allRoles", userService.findAllRoles());
        return "admin/user_edit_form";
    }

    @PostMapping("/users/update/{id}")
    public String updateUserRoles(@PathVariable Long id,
                               @RequestParam(name = "roles", required = false) Set<Long> roleIds,
                               @AuthenticationPrincipal UserDetails currentUserDetails,
                               RedirectAttributes redirectAttributes) {
        User userToUpdate = userService.findUserById(id).orElse(null);

        if (userToUpdate == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Пользователь для обновления не найден.");
            return "redirect:/admin/users";
        }

        boolean isSelf = currentUserDetails.getUsername().equals(userToUpdate.getUsername());
        boolean isLastAdmin = userService.isLastAdmin(userToUpdate.getId());
        
        Set<com.example.coffeeshop.model.Role> newRoles = new HashSet<>();
        if (roleIds != null && !roleIds.isEmpty()) {
            newRoles = roleIds.stream()
                    .map(roleId -> userService.findRoleById(roleId).orElse(null))
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());
        }

        if (isSelf && isLastAdmin && !newRoles.stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN"))) {
            redirectAttributes.addFlashAttribute("errorMessage", "Вы не можете снять с себя роль ADMIN, если вы единственный администратор.");
            return "redirect:/admin/users/edit/" + id;
        }
        if (newRoles.isEmpty()){
             redirectAttributes.addFlashAttribute("errorMessage", "Пользователь должен иметь хотя бы одну роль.");
            return "redirect:/admin/users/edit/" + id;
        }


        userService.updateUserRoles(id, newRoles);
        redirectAttributes.addFlashAttribute("successMessage", "Роли пользователя '" + userToUpdate.getUsername() + "' успешно обновлены!");
        return "redirect:/admin/users";
    }


    @GetMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, @AuthenticationPrincipal UserDetails currentUserDetails, RedirectAttributes redirectAttributes) {
        User userToDelete = userService.findUserById(id).orElse(null);

        if (userToDelete == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Пользователь с ID " + id + " не найден.");
            return "redirect:/admin/users";
        }

        if (currentUserDetails.getUsername().equals(userToDelete.getUsername())) {
            redirectAttributes.addFlashAttribute("errorMessage", "Вы не можете удалить свой собственный аккаунт.");
            return "redirect:/admin/users";
        }
        
        if (userService.isLastAdmin(id)) {
             redirectAttributes.addFlashAttribute("errorMessage", "Нельзя удалить последнего администратора.");
            return "redirect:/admin/users";
        }

        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "Пользователь '" + userToDelete.getUsername() + "' успешно удален!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка удаления пользователя: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ----- Управление Заказами (Orders) -----
    @GetMapping("/orders")
    public String listOrders(Model model) {
        List<Order> orders = orderService.findAllOrders();
        model.addAttribute("orders", orders);
        return "admin/orders_list";
    }

    @PostMapping("/orders/update-status/{orderId}")
    public String updateOrderStatus(@PathVariable Long orderId,
                                    @RequestParam OrderStatus status,
                                    RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatus(orderId, status);
            redirectAttributes.addFlashAttribute("successMessage", "Статус заказа #" + orderId + " успешно обновлен!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Ошибка обновления статуса заказа #" + orderId + ": " + e.getMessage());
        }
        return "redirect:/admin/orders";
    }
} 