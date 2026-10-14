package com.hml.museum.controller;

import com.hml.museum.dto.AuthDtos;
import com.hml.museum.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public AuthDtos.LoginResponse login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        authService.logout(bearerToken(authorization));
        return Map.of("success", true);
    }

    @PostMapping("/heartbeat")
    public AuthDtos.LoginResponse heartbeat(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return authService.heartbeat(bearerToken(authorization));
    }

    @PostMapping("/unlock")
    public AuthDtos.UserProfile unlock(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                       @Valid @RequestBody AuthDtos.UnlockRequest request) {
        return authService.unlock(bearerToken(authorization), request.password());
    }

    @PostMapping("/change-password")
    public Map<String, Object> changePassword(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
                                              @Valid @RequestBody AuthDtos.PasswordRequest request) {
        authService.changePassword(bearerToken(authorization), request);
        return Map.of("success", true);
    }

    @GetMapping("/me")
    public AuthDtos.UserProfile me(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        var user = authService.currentUser(bearerToken(authorization));
        return new AuthDtos.UserProfile(user.userId(), user.username(), user.roles(), false);
    }

    private static String bearerToken(String header) {
        if (header == null || !header.startsWith("Bearer ")) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "缺少登录令牌");
        }
        return header.substring(7).trim();
    }
}
