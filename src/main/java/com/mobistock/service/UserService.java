package com.mobistock.service;

import com.mobistock.model.User;
import com.mobistock.model.enums.Role;
import com.mobistock.repository.AuditLogRepository;
import com.mobistock.repository.PhoneRepository;
import com.mobistock.repository.SaleRepository;
import com.mobistock.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;
    private final SaleRepository saleRepository;
    private final PhoneRepository phoneRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    public User getUserById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public Optional<User> getUserByUsername(String username) {
        return repository.findByUsername(username);
    }

    public List<User> getAllUsers() {
        return repository.findAll();
    }

    @Transactional
    public User createUser(String username, String password, String fullName, Role role) {
        if (repository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setRole(role);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        return repository.save(user);
    }

    @Transactional
    public User updateUser(Long id, String fullName, Role role, Boolean isActive) {
        User user = getUserById(id);
        if ("admin".equalsIgnoreCase(user.getUsername())) {
            user.setFullName(fullName);
            user.setRole(Role.ADMIN);
            user.setIsActive(true);
        } else {
            user.setFullName(fullName);
            user.setRole(role);
            user.setIsActive(isActive != null ? isActive : false);
        }
        return repository.save(user);
    }

    @Transactional
    public void deleteUser(Long id, User currentUser) {
        User userToDelete = getUserById(id);
        if ("admin".equalsIgnoreCase(userToDelete.getUsername())) {
            throw new RuntimeException("The primary administrator account 'admin' cannot be deleted.");
        }
        if (currentUser != null && currentUser.getId().equals(id)) {
            throw new RuntimeException("You cannot delete your own account while logged in.");
        }

        User primaryAdmin = repository.findByUsername("admin")
                .orElseThrow(() -> new RuntimeException("Primary admin account 'admin' not found to reassign records."));

        saleRepository.reassignSales(userToDelete, primaryAdmin);
        phoneRepository.reassignPhones(userToDelete, primaryAdmin);
        auditLogRepository.reassignLogs(userToDelete, primaryAdmin);

        repository.delete(userToDelete);
    }

    @Transactional
    public void changePassword(Long id, String newPassword) {
        User user = getUserById(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        repository.save(user);
    }

    public User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails) {
            username = ((UserDetails) principal).getUsername();
        } else {
            username = principal.toString();
        }
        return repository.findByUsername(username).orElse(null);
    }
}
