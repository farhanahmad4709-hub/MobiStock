package com.mobistock.config;

import com.mobistock.model.User;
import com.mobistock.model.enums.Role;
import com.mobistock.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        User admin = null;
        if (!userRepository.existsByUsername("admin")) {
            admin = User.builder()
                    .username("admin")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .fullName("Administrator")
                    .role(Role.ADMIN)
                    .isActive(true)
                    .build();
            admin = userRepository.save(admin);
            log.info("Created default admin user (username: admin, password: admin123)");
        } else {
            admin = userRepository.findByUsername("admin").orElse(null);
        }

        // Test data initialization removed to prevent dummy phones returning.


    }
}
