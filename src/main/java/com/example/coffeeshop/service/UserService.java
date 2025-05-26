package com.example.coffeeshop.service;

import com.example.coffeeshop.model.User;
import com.example.coffeeshop.model.Role;
import com.example.coffeeshop.repository.UserRepository;
import com.example.coffeeshop.repository.RoleRepository;
import com.example.coffeeshop.dto.UserUpdateProfileDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User registerUser(String username, String email, String password) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        Role userRole = roleRepository.findByName("ROLE_USER").orElseThrow(() -> new RuntimeException("Error: Role USER is not found."));
        user.setRoles(new HashSet<>() {{ add(userRole); }});
        return userRepository.save(user);
    }

    // Методы для админки
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> findUserById(Long id) {
        return userRepository.findById(id);
    }

    public User updateUser(User user) { // Общий метод обновления
        // Пароль не меняем здесь, если он не предоставлен или пуст в DTO (если будем использовать DTO)
        // Для простоты, пока прямой save
        return userRepository.save(user);
    }

    @Transactional
    public User updateUserRoles(Long userId, Set<Role> newRoles) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        user.setRoles(newRoles);
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    public List<Role> findAllRoles() { // Для формы редактирования ролей пользователя
        return roleRepository.findAll();
    }

    public Optional<Role> findRoleById(Long id) {
        return roleRepository.findById(id);
    }

    public boolean isLastAdmin(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN"))) {
            return false; // Не админ, или не существует
        }
        List<User> allAdmins = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN")))
                .collect(Collectors.toList());
        return allAdmins.size() == 1 && allAdmins.get(0).getId().equals(userId);
    }

    @Transactional
    public User updateUserProfile(String currentUsername, UserUpdateProfileDto userUpdateDto) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("User not found: " + currentUsername));

        // Проверяем, если email меняется и новый email уже занят кем-то другим
        if (!user.getEmail().equals(userUpdateDto.getEmail())) {
            userRepository.findByEmail(userUpdateDto.getEmail()).ifPresent(existingUser -> {
                if (!existingUser.getId().equals(user.getId())) {
                    throw new RuntimeException("Email " + userUpdateDto.getEmail() + " is already taken.");
                }
            });
            user.setEmail(userUpdateDto.getEmail());
        }

        user.setFirstName(userUpdateDto.getFirstName());
        user.setLastName(userUpdateDto.getLastName());
        // Пароль и роли здесь не меняем

        return userRepository.save(user);
    }
} 