package com.hml.museum.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SuperUserInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String initialPassword;

    public SuperUserInitializer(JdbcTemplate jdbc, PasswordEncoder passwordEncoder,
                                @Value("${museum.auth.super-username:super}") String username,
                                @Value("${museum.auth.super-password:}") String initialPassword) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.username = username.trim();
        this.initialPassword = initialPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM app_user_role WHERE role_code='SUPER'", Integer.class);
        if (count != null && count > 0) return;
        if (initialPassword == null || initialPassword.length() < 12) {
            throw new IllegalStateException(
                    "首次启动必须配置 MUSEUM_SUPER_PASSWORD，且长度至少为 12 位；请勿使用固定默认密码。");
        }

        Long userId;
        var existing = jdbc.queryForList("SELECT id FROM app_user WHERE username=?", username);
        if (existing.isEmpty()) {
            jdbc.update("INSERT INTO app_user(username, password_hash, enabled) VALUES (?, ?, 1)",
                    username, passwordEncoder.encode(initialPassword));
            userId = jdbc.queryForObject("SELECT id FROM app_user WHERE username=?", Long.class, username);
        } else {
            userId = ((Number) existing.get(0).get("id")).longValue();
            jdbc.update("UPDATE app_user SET enabled=1 WHERE id=?", userId);
        }
        jdbc.update("INSERT IGNORE INTO app_user_role(user_id, role_code) VALUES (?, 'SUPER')", userId);
    }
}
