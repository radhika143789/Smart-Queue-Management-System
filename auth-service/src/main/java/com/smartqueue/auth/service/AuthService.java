package com.smartqueue.auth.service;

import com.smartqueue.auth.dto.AuthResponse;
import com.smartqueue.auth.dto.LoginRequest;
import com.smartqueue.auth.dto.RegisterRequest;
import com.smartqueue.auth.dto.UserProfileResponse;
import com.smartqueue.auth.entity.OAuthProviderEntity;
import com.smartqueue.auth.entity.RefreshTokenEntity;
import com.smartqueue.auth.entity.RoleEntity;
import com.smartqueue.auth.entity.UserEntity;
import com.smartqueue.auth.entity.UserRole;
import com.smartqueue.auth.repository.OAuthProviderRepository;
import com.smartqueue.auth.repository.RefreshTokenRepository;
import com.smartqueue.auth.repository.RoleRepository;
import com.smartqueue.auth.repository.UserRepository;
import com.smartqueue.auth.util.JwtUtil;
import com.smartqueue.common.enums.ErrorCode;
import com.smartqueue.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OAuthProviderRepository oauthProviderRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${app.jwt.access-expiry-ms}")
    private long accessExpiryMs;

    @Value("${app.jwt.refresh-expiry-ms}")
    private long refreshExpiryMs;

    @Transactional
    public UserEntity register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS, "Email already registered: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS, "Username already taken: " + request.getUsername());
        }

        RoleEntity userRole = roleRepository.findByName(UserRole.USER)
                .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_ERROR, "Default USER role not seeded in database"));

        UserEntity user = UserEntity.builder()
                .email(request.getEmail())
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .roles(new HashSet<>(Collections.singletonList(userRole)))
                .build();

        return userRepository.save(user);
    }

    /**
     * Register a new user and immediately issue auth tokens.
     * Avoids the anti-pattern of calling login() after register (which would re-hash password comparison).
     */
    @Transactional
    public AuthResponse registerAndLogin(RegisterRequest request, String ipAddress, String deviceInfo) {
        UserEntity user = register(request);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        return generateAuthResponse(user, ipAddress, deviceInfo);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String deviceInfo) {
        // Use a generic message to prevent user enumeration attacks
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        if (!user.isAccountNonLocked()) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS,
                    "Account is temporarily locked due to too many failed login attempts. Try again later.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= 5) {
                user.setLockedUntil(Instant.now().plusSeconds(900)); // 15 mins lock
                log.warn("Account locked for user {} after {} failed attempts", user.getEmail(), attempts);
            }
            userRepository.save(user);
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        // Reset failures on successful login
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        return generateAuthResponse(user, ipAddress, deviceInfo);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenStr) {
        RefreshTokenEntity tokenEntity = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new AppException(ErrorCode.REFRESH_TOKEN_EXPIRED, "Refresh token not found or already revoked"));

        if (!tokenEntity.isValid()) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_EXPIRED, "Refresh token has expired or been revoked");
        }

        UserEntity user = tokenEntity.getUser();

        // Rotate: revoke old token, issue new one
        tokenEntity.setRevoked(true);
        refreshTokenRepository.save(tokenEntity);

        return generateAuthResponse(user, tokenEntity.getIpAddress(), tokenEntity.getDeviceInfo());
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "User not found: " + userId));

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .profilePicture(user.getProfilePicture())
                .roles(user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toList()))
                .createdAt(user.getCreatedAt())
                .emailVerified(user.isEmailVerified())
                .build();
    }

    @Transactional
    public AuthResponse processOAuth2Login(OAuth2User oauth2User, String provider) {
        String email = oauth2User.getAttribute("email");
        String providerId = oauth2User.getAttribute("sub");
        if (providerId == null) {
            providerId = oauth2User.getAttribute("id"); // Fallback for other providers
        }

        if (email == null) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS, "Email not provided by OAuth2 provider");
        }

        Optional<OAuthProviderEntity> existingProviderOpt = oauthProviderRepository.findByProviderAndProviderUserId(provider, providerId);
        
        UserEntity user;
        if (existingProviderOpt.isPresent()) {
            user = existingProviderOpt.get().getUser();
        } else {
            // Check if user exists by email
            Optional<UserEntity> userOpt = userRepository.findByEmail(email);
            if (userOpt.isPresent()) {
                user = userOpt.get();
            } else {
                // Create new user
                RoleEntity userRole = roleRepository.findByName(UserRole.USER)
                        .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_ERROR, "Default USER role not seeded"));
                
                String firstName = oauth2User.getAttribute("given_name");
                String lastName = oauth2User.getAttribute("family_name");
                if (firstName == null) firstName = "User";
                if (lastName == null) lastName = "";

                user = UserEntity.builder()
                        .email(email)
                        .username(email.split("@")[0] + "_" + UUID.randomUUID().toString().substring(0, 5))
                        .firstName(firstName)
                        .lastName(lastName)
                        .profilePicture(oauth2User.getAttribute("picture"))
                        .emailVerified(true)
                        .roles(new HashSet<>(Collections.singletonList(userRole)))
                        .build();
                user = userRepository.save(user);
            }

            OAuthProviderEntity providerEntity = OAuthProviderEntity.builder()
                    .provider(provider)
                    .providerUserId(providerId)
                    .user(user)
                    .email(email)
                    .build();
            oauthProviderRepository.save(providerEntity);
        }

        return generateAuthResponse(user, "oauth2", "oauth2");
    }

    private AuthResponse generateAuthResponse(UserEntity user, String ip, String deviceInfo) {
        // Collect roles as a Set<String> for inclusion in JWT claims
        Set<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        // Generate access token with userId + roles embedded as claims
        String accessToken = jwtUtil.generateAccessToken(user, user.getId(), roles);

        // Refresh token is a secure random UUID pair (not a JWT — stored in DB)
        String refreshToken = UUID.randomUUID() + "-" + UUID.randomUUID();

        RefreshTokenEntity refreshTokenEntity = RefreshTokenEntity.builder()
                .token(refreshToken)
                .user(user)
                .expiresAt(Instant.now().plusMillis(refreshExpiryMs))
                .deviceInfo(deviceInfo)
                .ipAddress(ip)
                .build();
        refreshTokenRepository.save(refreshTokenEntity);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessExpiryMs / 1000)
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .roles(user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toList()))
                .build();
    }
}
