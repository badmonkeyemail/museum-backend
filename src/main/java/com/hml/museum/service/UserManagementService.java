package com.hml.museum.service;

import com.hml.museum.dto.AuthDtos;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Service
public class UserManagementService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public UserManagementService(JdbcTemplate jdbc, PasswordEncoder passwordEncoder, AuthService authService) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    public List<AuthDtos.UserListItem> listUsers() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, username, enabled, created_at FROM app_user ORDER BY id");
        return rows.stream().map(row -> {
            Long id = ((Number) row.get("id")).longValue();
            Object created = row.get("created_at");
            LocalDateTime createdAt = created instanceof java.sql.Timestamp ts ? ts.toLocalDateTime()
                    : created instanceof LocalDateTime dt ? dt : null;
            return new AuthDtos.UserListItem(id, (String) row.get("username"),
                    authService.roles(id), asBoolean(row.get("enabled")), createdAt);
        }).toList();
    }

    @Transactional
    public AuthDtos.UserListItem createAdmin(AuthDtos.UserCreateRequest request) {
        String username = request.username().trim();
        if (username.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名不能为空");
        try {
            jdbc.update("INSERT INTO app_user(username, password_hash, enabled) VALUES (?, ?, 1)",
                    username, passwordEncoder.encode(request.initialPassword()));
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
        }
        Long id = jdbc.queryForObject("SELECT id FROM app_user WHERE username=?", Long.class, username);
        jdbc.update("INSERT INTO app_user_role(user_id, role_code) VALUES (?, 'ADMIN')", id);
        return new AuthDtos.UserListItem(id, username, authService.roles(id), true, nowUtc());
    }

    @Transactional
    public void setEnabled(long id, boolean enabled, long operatorId) {
        ensureUserExists(id);
        if (hasSuperRole(id) && id != operatorId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只能管理 ADMIN 用户，不能修改其他超级用户");
        }
        if (id == operatorId && !enabled) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不能停用当前登录用户");
        }
        if (!enabled) {
            jdbc.update("UPDATE app_active_session SET user_id=NULL, token_hash=NULL, issued_at=NULL, last_seen_at=NULL, expires_at=NULL WHERE id=1 AND user_id=?", id);
        }
        jdbc.update("UPDATE app_user SET enabled=?, updated_at=? WHERE id=?", enabled, nowUtc(), id);
    }

    @Transactional
    public void setAdminRole(long id, boolean enabled, long operatorId) {
        ensureUserExists(id);
        boolean targetIsSuper = jdbc.queryForList(
                "SELECT role_code FROM app_user_role WHERE user_id=? AND role_code='SUPER'", id).size() > 0;
        if (targetIsSuper && id != operatorId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不能修改其他超级用户的权限");
        }
        if (enabled) {
            jdbc.update("INSERT IGNORE INTO app_user_role(user_id, role_code) VALUES (?, 'ADMIN')", id);
        } else {
            jdbc.update("DELETE FROM app_user_role WHERE user_id=? AND role_code='ADMIN'", id);
            if (jdbc.queryForList("SELECT role_code FROM app_user_role WHERE user_id=?", id).isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户至少必须保留一种权限");
            }
        }
    }

    @Transactional
    public void resetPassword(long id, String newPassword, long operatorId) {
        ensureUserExists(id);
        if (hasSuperRole(id) && id != operatorId) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只能重置 ADMIN 用户密码，不能重置其他超级用户密码");
        }
        jdbc.update("UPDATE app_user SET password_hash=?, updated_at=? WHERE id=?",
                passwordEncoder.encode(newPassword), nowUtc(), id);
        // A password reset invalidates any session belonging to that user.
        jdbc.update("UPDATE app_active_session SET user_id=NULL, token_hash=NULL, issued_at=NULL, last_seen_at=NULL, expires_at=NULL WHERE id=1 AND user_id=?", id);
    }

    private boolean hasSuperRole(long id) {
        return !jdbc.queryForList(
                "SELECT role_code FROM app_user_role WHERE user_id=? AND role_code='SUPER'", id).isEmpty();
    }

    private void ensureUserExists(long id) {
        if (jdbc.queryForList("SELECT id FROM app_user WHERE id=?", id).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "用户不存在");
        }
    }

    private static boolean asBoolean(Object value) {
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.intValue() != 0;
        return value != null && Boolean.parseBoolean(value.toString());
    }

    private static LocalDateTime nowUtc() { return LocalDateTime.now(ZoneOffset.UTC); }
}
