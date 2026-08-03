package com.smartqueue.auth.controller;

import com.smartqueue.auth.dto.AuthResponse;
import com.smartqueue.auth.dto.LoginRequest;
import com.smartqueue.auth.dto.RegisterRequest;
import com.smartqueue.auth.dto.TokenRefreshRequest;
import com.smartqueue.auth.dto.UserProfileResponse;
import com.smartqueue.auth.entity.UserEntity;
import com.smartqueue.auth.service.AuthService;
import com.smartqueue.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication operations.
 * All responses are wrapped in the standard {@link ApiResponse} envelope.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * Register a new user and immediately issue tokens (no second login required).
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest) {
        log.info("Registering new user: {}", request.getEmail());

        String ipAddress = getClientIp(servletRequest);
        String deviceInfo = servletRequest.getHeader("User-Agent");

        // Register then generate tokens in a single service call (avoids re-encoding password)
        AuthResponse authResponse = authService.registerAndLogin(request, ipAddress, deviceInfo);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", authResponse));
    }

    /**
     * Authenticate with email + password and receive JWT access + refresh tokens.
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {
        log.info("Login attempt for: {}", request.getEmail());
        String ipAddress = getClientIp(servletRequest);
        String deviceInfo = servletRequest.getHeader("User-Agent");

        AuthResponse authResponse = authService.login(request, ipAddress, deviceInfo);
        return ResponseEntity.ok(ApiResponse.success("Login successful", authResponse));
    }

    /**
     * Rotate refresh token — issues a new access token and rotates the refresh token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {
        log.info("Token refresh requested");
        AuthResponse authResponse = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", authResponse));
    }

    /**
     * Revoke the provided refresh token (logout from current device).
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody TokenRefreshRequest request) {
        log.info("Logout requested");
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    /**
     * Get the authenticated user's profile. Requires valid JWT.
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserEntity)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH_006", "Not authenticated"));
        }

        UserEntity principal = (UserEntity) authentication.getPrincipal();
        UserProfileResponse profile = authService.getUserProfile(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    /**
     * Health check (also exposed via /actuator/health).
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Auth service is healthy", "UP"));
    }

    /**
     * Extract the real client IP, respecting X-Forwarded-For from reverse proxies.
     */
    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank()) {
            return request.getRemoteAddr();
        }
        // Take only the first IP (client IP), ignore proxy chain
        return xfHeader.split(",")[0].trim();
    }
}
