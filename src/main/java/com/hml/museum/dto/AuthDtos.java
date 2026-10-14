package com.hml.museum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank @Size(max = 100) String username,
            @NotBlank @Size(max = 200) String password) {}

    public record LoginResponse(
            String token,
            String tokenType,
            LocalDateTime expiresAt,
            UserProfile user,
            int idleTimeoutSeconds,
            int heartbeatIntervalSeconds) {}

    public record UserProfile(Long id, String username, List<String> roles, boolean mustChangePassword) {}

    public record PasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 12, max = 200) String newPassword) {}

    public record UnlockRequest(@NotBlank String password) {}

    public record UserCreateRequest(
            @NotBlank @Size(min = 3, max = 100) String username,
            @NotBlank @Size(min = 12, max = 200) String initialPassword) {}

    public record UserStatusRequest(boolean enabled) {}

    public record AdminRoleRequest(boolean enabled) {}

    public record UserListItem(Long id, String username, List<String> roles,
                               boolean enabled, LocalDateTime createdAt) {}
}
