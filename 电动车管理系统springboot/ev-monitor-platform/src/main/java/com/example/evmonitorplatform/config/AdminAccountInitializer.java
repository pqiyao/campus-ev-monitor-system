package com.example.evmonitorplatform.config;

import com.example.evmonitorplatform.entity.User;
import com.example.evmonitorplatform.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAccountInitializer implements ApplicationRunner {

    private static final String DEFAULT_ADMIN_USERNAME = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "123456";

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        try {
            User admin = userMapper.selectByUsername(DEFAULT_ADMIN_USERNAME);
            if (admin == null) {
                createDefaultAdmin();
                return;
            }

            boolean updated = false;
            if (!isDefaultPassword(admin.getPassword())) {
                admin.setPassword(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
                updated = true;
            }
            if (!"active".equalsIgnoreCase(admin.getStatus())) {
                admin.setStatus("active");
                updated = true;
            }
            if (updated) {
                admin.setUpdateTime(LocalDateTime.now());
                userMapper.updateById(admin);
                log.info("Reset existing admin account credentials to default: {} / {}", DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PASSWORD);
            } else {
                log.info("Default admin already ready: {}", DEFAULT_ADMIN_USERNAME);
            }
        } catch (Exception ex) {
            log.error("Failed to initialize default admin account: {}", ex.getMessage(), ex);
        }
    }

    private void createDefaultAdmin() {
        User defaultAdmin = new User();
        defaultAdmin.setUsername(DEFAULT_ADMIN_USERNAME);
        defaultAdmin.setPassword(passwordEncoder.encode(DEFAULT_ADMIN_PASSWORD));
        defaultAdmin.setRealName("System Admin");
        defaultAdmin.setUserType("admin");
        defaultAdmin.setPhone(generateFallbackPhone());
        defaultAdmin.setStatus("active");
        defaultAdmin.setDeleted(false);
        defaultAdmin.setCreateTime(LocalDateTime.now());
        defaultAdmin.setUpdateTime(LocalDateTime.now());
        userMapper.insert(defaultAdmin);
        log.info("Created default admin account: {} / {}", DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PASSWORD);
    }

    private boolean isDefaultPassword(String currentPassword) {
        if (currentPassword == null || currentPassword.isBlank()) {
            return false;
        }
        try {
            if (passwordEncoder.matches(DEFAULT_ADMIN_PASSWORD, currentPassword)) {
                return true;
            }
        } catch (IllegalArgumentException ignored) {
            // legacy non-bcrypt format
        }
        return DEFAULT_ADMIN_PASSWORD.equals(currentPassword);
    }

    private String generateFallbackPhone() {
        for (int i = 0; i < 10000; i++) {
            String phone = String.format("1990000%04d", i);
            if (userMapper.selectByPhone(phone) == null) {
                return phone;
            }
        }
        return "19999999999";
    }
}
