package com.mobistock.controller;

import com.mobistock.model.User;
import com.mobistock.model.enums.Role;
import com.mobistock.service.AuditService;
import com.mobistock.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserService userService;
    private final AuditService auditService;

    @GetMapping
    public String adminDashboard() {
        return "redirect:/admin/users";
    }

    @GetMapping("/users")
    public String listUsers(Model model) {
        model.addAttribute("currentUser", userService.getCurrentUser());
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("pageTitle", "User Management");
        return "admin/users";
    }

    @PostMapping("/users/add")
    public String addUser(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam String fullName,
                          @RequestParam Role role,
                          RedirectAttributes redirectAttributes) {
        try {
            userService.createUser(username, password, fullName, role);
            redirectAttributes.addFlashAttribute("success", "User created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/edit/{id}")
    public String editUser(@PathVariable Long id,
                           @RequestParam String fullName,
                           @RequestParam Role role,
                           @RequestParam(required = false) Boolean isActive,
                           HttpServletRequest request,
                           RedirectAttributes redirectAttributes) {
        try {
            boolean activeFlag = isActive != null && isActive;
            User currentUser = userService.getCurrentUser();
            userService.updateUser(id, fullName, role, activeFlag);
            
            // If the logged-in user deactivates their own account, immediately invalidate session and logout
            if (currentUser != null && currentUser.getId().equals(id) && !activeFlag) {
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                return "redirect:/login?error=inactive";
            }

            redirectAttributes.addFlashAttribute("success", "User updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.getCurrentUser();
            userService.deleteUser(id, currentUser);
            redirectAttributes.addFlashAttribute("success", "User deleted successfully. Existing records were safely preserved.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @GetMapping("/audit-log")
    public String auditLog(Model model) {
        model.addAttribute("logs", auditService.getAllLogs());
        model.addAttribute("pageTitle", "Audit Log");
        return "admin/audit-log";
    }
}
