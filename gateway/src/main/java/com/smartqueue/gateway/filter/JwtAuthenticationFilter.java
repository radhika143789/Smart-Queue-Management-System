package com.smartqueue.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@Slf4j
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.public-paths}")
    private List<String> publicPaths;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    @Data
    public static class Config {
        // empty config
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            if (isPublicPath(path)) {
                return chain.filter(exchange);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Missing or malformed Authorization header");
            }

            String token = authHeader.substring(7);
            try {
                // FIX (BUG #8): Must use BASE64.decode() to match auth-service JwtUtil which also
                // uses Decoders.BASE64.decode(). Using getBytes(UTF_8) caused SignatureException
                // on every request, rejecting all valid JWTs.
                SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                // FIX: Reject refresh tokens at the gateway — only ACCESS tokens are valid for API calls
                String tokenType = claims.get("tokenType", String.class);
                if ("REFRESH".equals(tokenType)) {
                    log.warn("Refresh token used for API access from path: {}", path);
                    return onError(exchange, "Refresh tokens cannot be used for API access");
                }

                String userId = claims.getSubject();
                String roles = claims.get("roles", String.class);
                String email = claims.get("email", String.class);

                if (userId == null) {
                    return onError(exchange, "Token is missing required claims");
                }

                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                        .header("X-User-Id", userId)
                        .header("X-User-Roles", roles != null ? roles : "")
                        .header("X-User-Email", email != null ? email : "")
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                log.warn("Expired JWT for path {}: {}", path, e.getMessage());
                return onError(exchange, "Token has expired");
            } catch (io.jsonwebtoken.security.SignatureException e) {
                log.warn("Invalid JWT signature for path {}: {}", path, e.getMessage());
                return onError(exchange, "Invalid token signature");
            } catch (Exception e) {
                log.error("JWT validation failed for path {}: {}", path, e.getMessage());
                return onError(exchange, "Invalid or missing token");
            }
        };
    }

    private boolean isPublicPath(String path) {
        return publicPaths != null && publicPaths.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String json = String.format("{\"success\":false,\"error\":{\"code\":\"AUTH_004\",\"message\":\"%s\"}}", err);
        DataBuffer buffer = response.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
