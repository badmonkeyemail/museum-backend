package com.hml.museum.service;

import com.hml.museum.dto.AuthDtos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class AuthService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final int leaseSeconds;
    private final int idleTimeoutSeconds;
    private final int heartbeatIntervalSeconds;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(JdbcTemplate jdbc, PasswordEncoder passwordEncoder,
                      @Value("${museum.auth.session-lease-seconds:120}") int leaseSeconds,
                      @Value("${museum.auth.idle-timeout-seconds:900}") int idleTimeoutSeconds,
                      @Value("${museum.auth.heartbeat-interval-seconds:30}") int heartbeatIntervalSeconds) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.leaseSeconds = Math.max(45, leaseSeconds);
        this.idleTimeoutSeconds = Math.max(60, idleTimeoutSeconds);
        this.heartbeatIntervalSeconds = Math.max(15, Math.min(heartbeatIntervalSeconds, this.leaseSeconds / 2));
    }

    public record AuthenticatedUser(Long userId, String username, List<String> roles) {}

    @Transactional
    public AuthDtos.LoginResponse login(AuthDtos.LoginRequest request) {
        Map<String, Object> user;
        try {
            user = jdbc.queryForMap("SELECT id, username, password_hash, enabled FROM app_user WHERE username = ?",
                    request.username().trim());
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!Boolean.TRUE.equals(asBoolean(user.get("enabled")))
                || !passwordEncoder.matches(request.password(), (String) user.get("password_hash"))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }

        // Lock the singleton row to serialize concurrent login attempts across application instances.
        jdbc.queryForObject("SELECT id FROM app_active_session WHERE id = 1 FOR UPDATE", Integer.class);
        LocalDateTime now = nowUtc();
        Integer activeCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM app_active_session WHERE id=1 AND user_id IS NOT NULL AND expires_at > ?",
                Integer.class, now);
        if (activeCount != null && activeCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "系统正在被其他用户使用，请稍后再试");
        }

        String token = newToken();
        LocalDateTime expiresAt = now.plusSeconds(leaseSeconds);
        Long userId = ((Number) user.get("id")).longValue();
        jdbc.update("UPDATE app_active_session SET user_id=?, token_hash=?, issued_at=?, last_seen_at=?, expires_at=? WHERE id=1",
                userId, sha256(token), now, now, expiresAt);
        List<String> roles = roles(userId);
        return new AuthDtos.LoginResponse(token, "Bearer", expiresAt,
                new AuthDtos.UserProfile(userId, (String) user.get("username"), roles, false),
                idleTimeoutSeconds, heartbeatIntervalSeconds);
    }

    public AuthenticatedUser validateAndTouch(String token) {
        if (token == null || token.isBlank()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        String hash = sha256(token);
        LocalDateTime now = nowUtc();
        int updated = jdbc.update("UPDATE app_active_session SET last_seen_at=?, expires_at=? " +
                        "WHERE id=1 AND token_hash=? AND expires_at > ?",
                now, now.plusSeconds(leaseSeconds), hash, now);
        if (updated != 1) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "会话已过期");
        Map<String, Object> row = jdbc.queryForMap(
                "SELECT u.id, u.username, u.enabled FROM app_active_session s " +
                        "JOIN app_user u ON u.id=s.user_id WHERE s.id=1 AND s.token_hash=? AND s.expires_at > ?",
                hash, now);
        if (!Boolean.TRUE.equals(asBoolean(row.get("enabled")))) {
            releaseToken(hash);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户已停用");
        }
        Long userId = ((Number) row.get("id")).longValue();
        return new AuthenticatedUser(userId, (String) row.get("username"), roles(userId));
    }

    public AuthDtos.LoginResponse heartbeat(String token) {
        AuthenticatedUser user = validateAndTouch(token);
        LocalDateTime expiresAt = nowUtc().plusSeconds(leaseSeconds);
        return new AuthDtos.LoginResponse(null, "Bearer", expiresAt,
                new AuthDtos.UserProfile(user.userId(), user.username(), user.roles(), false),
                idleTimeoutSeconds, heartbeatIntervalSeconds);
    }

    public void logout(String token) {
        if (token == null || token.isBlank()) return;
        releaseToken(sha256(token));
    }

    @Transactional
    public AuthDtos.UserProfile unlock(String token, String password) {
        AuthenticatedUser user = validateAndTouch(token);
        String hash;
        try {
            hash = jdbc.queryForObject("SELECT password_hash FROM app_user WHERE id=? AND enabled=1",
                    String.class, user.userId());
        } catch (EmptyResultDataAccessException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户已停用");
        }
        if (!passwordEncoder.matches(password, hash)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "密码错误");
        }
        return new AuthDtos.UserProfile(user.userId(), user.username(), user.roles(), false);
    }

    @Transactional
    public void changePassword(String token, AuthDtos.PasswordRequest request) {
        AuthenticatedUser user = validateAndTouch(token);
        String currentHash = jdbc.queryForObject("SELECT password_hash FROM app_user WHERE id=?",
                String.class, user.userId());
        if (!passwordEncoder.matches(request.currentPassword(), currentHash)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前密码不正确");
        }
        if (request.currentPassword().equals(request.newPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "新密码不能与当前密码相同");
        }
        jdbc.update("UPDATE app_user SET password_hash=?, updated_at=? WHERE id=?",
                passwordEncoder.encode(request.newPassword()), nowUtc(), user.userId());
    }

    public AuthenticatedUser currentUser(String token) {
        return validateAndTouch(token);
    }

    public int idleTimeoutSeconds() { return idleTimeoutSeconds; }

    public List<String> roles(Long userId) {
        return jdbc.query("SELECT role_code FROM app_user_role WHERE user_id=? ORDER BY role_code",
                (rs, n) -> rs.getString(1), userId);
    }

    private void releaseToken(String hash) {
        jdbc.update("UPDATE app_active_session SET user_id=NULL, token_hash=NULL, issued_at=NULL, last_seen_at=NULL, expires_at=NULL " +
                "WHERE id=1 AND token_hash=?", hash);
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private static LocalDateTime nowUtc() { return LocalDateTime.now(ZoneOffset.UTC); }

    private static Boolean asBoolean(Object value) {
        if (value instanceof Boolean b) return b;
        if (value instanceof Number n) return n.intValue() != 0;
        return value != null && Boolean.parseBoolean(value.toString());
    }
}
