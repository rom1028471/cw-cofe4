package com.example.coffeeshop.controller;

import com.example.coffeeshop.model.Order;
import com.example.coffeeshop.service.OrderService;
import com.example.coffeeshop.service.UserService;
import com.example.coffeeshop.dto.UserUpdateProfileDto;
import com.example.coffeeshop.model.User;
import com.example.coffeeshop.service.LoyaltyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class ProfileController {

    private final OrderService orderService;
    private final UserService userService;
    private final LoyaltyService loyaltyService;

    @Autowired
    public ProfileController(OrderService orderService, UserService userService, LoyaltyService loyaltyService) {
        this.orderService = orderService;
        this.userService = userService;
        this.loyaltyService = loyaltyService;
    }

    @GetMapping("/profile")
    public String profilePage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        String username = userDetails.getUsername();
        model.addAttribute("username", username);

        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        model.addAttribute("user", user);
        
        List<Order> orders = orderService.getOrdersForUser(username);
        model.addAttribute("orders", orders);
        
        LoyaltyService.LoyaltyProgression progression = loyaltyService.getLoyaltyProgression(username);
        model.addAttribute("loyaltyProgression", progression);
        
        return "profile";
    }

    @GetMapping("/profile/edit")
    public String editProfilePage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        String username = userDetails.getUsername();
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        UserUpdateProfileDto dto = new UserUpdateProfileDto(user.getFirstName(), user.getLastName(), user.getEmail());
        model.addAttribute("userUpdateDto", dto);
        model.addAttribute("username", username);
        return "profile-edit";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                @ModelAttribute("userUpdateDto") UserUpdateProfileDto userUpdateDto,
                                RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        String username = userDetails.getUsername();
        try {
            userService.updateUserProfile(username, userUpdateDto);
            redirectAttributes.addFlashAttribute("successMessage", "Профиль успешно обновлен.");
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/profile/edit";
        }
        return "redirect:/profile";
    }
} 