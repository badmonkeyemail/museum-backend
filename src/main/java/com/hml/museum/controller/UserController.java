package com.hml.museum.controller;

import com.hml.museum.dto.AuthDtos;
import com.hml.museum.service.UserManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER')")
public class UserController {
    private final UserManagementService service;

    @GetMapping
    public List<AuthDtos.UserListItem> list() {
        return service.listUsers();
    }

    @PostMapping
    public AuthDtos.UserListItem create(@Valid @RequestBody AuthDtos.UserCreateRequest request) {
        return service.createAdmin(request);
    }

    @PatchMapping("/{id}/enabled")
    public Map<String, Object> setEnabled(@PathVariable long id,
                                          @Valid @RequestBody AuthDtos.UserStatusRequest request,
                                          Authentication authentication) {
        service.setEnabled(id, request.enabled(), operatorId(authentication));
        return Map.of("success", true);
    }

    @PatchMapping("/{id}/admin-role")
    public Map<String, Object> setAdminRole(@PathVariable long id,
                                            @Valid @RequestBody AuthDtos.AdminRoleRequest request,
                                            Authentication authentication) {
        service.setAdminRole(id, request.enabled(), operatorId(authentication));
        return Map.of("success", true);
    }

    @PutMapping("/{id}/password")
    public Map<String, Object> resetPassword(@PathVariable long id,
                                             @Valid @RequestBody PasswordResetRequest request) {
        service.resetPassword(id, request.newPassword());
        return Map.of("success", true);
    }

    public record PasswordResetRequest(@NotBlank @Size(min = 12, max = 200) String newPassword) {}

    private long operatorId(Authentication authentication) {
        // The bearer filter places the numeric ID in details; the username is used only as a fallback lookup.
        Object principal = authentication.getDetails();
        if (principal instanceof Number n) return n.longValue();
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED, "无法识别当前用户");
    }
}
