package com.smartqueue.auth.unit;

import com.smartqueue.auth.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Base64 encoded 64+ char secret for HS512
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "dGVzdC1qd3Qtc2VjcmV0LWtleS10aGF0LWlzLWxvbmctZW5vdWdoLWZvci1IUzUxMi1hbGdvcml0aG0=");
        ReflectionTestUtils.setField(jwtUtil, "accessExpiryMs", 900000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshExpiryMs", 604800000L);
    }

    @Test
    @DisplayName("generateAccessToken - should contain userId and roles claims")
    void shouldGenerateAccessTokenWithClaims() {
        UserDetails user = User.withUsername("test@test.com")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        String token = jwtUtil.generateAccessToken(user, 42L, Set.of("ROLE_USER"));

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("test@test.com");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(42L);
        List<String> roles = jwtUtil.extractRoles(token);
        assertThat(roles).containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("validateToken - should return true for valid token")
    void shouldValidateValidToken() {
        UserDetails user = User.withUsername("test@test.com")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        String token = jwtUtil.generateAccessToken(user, 1L, Set.of("ROLE_USER"));
        assertThat(jwtUtil.validateToken(token, user)).isTrue();
    }

    @Test
    @DisplayName("validateToken - should return false for token with wrong username")
    void shouldRejectTokenWithWrongUsername() {
        UserDetails user1 = User.withUsername("user1@test.com").password("").authorities(Collections.emptyList()).build();
        UserDetails user2 = User.withUsername("user2@test.com").password("").authorities(Collections.emptyList()).build();

        String token = jwtUtil.generateAccessToken(user1, 1L, Set.of());
        assertThat(jwtUtil.validateToken(token, user2)).isFalse();
    }

    @Test
    @DisplayName("generateRefreshToken - should not contain roles claim")
    void shouldGenerateRefreshTokenWithoutRoles() {
        String token = jwtUtil.generateRefreshToken("test@test.com");
        Claims claims = jwtUtil.extractAllClaims(token);

        assertThat(claims.get("roles")).isNull();
        assertThat(claims.get("tokenType")).isEqualTo("REFRESH");
    }
}
