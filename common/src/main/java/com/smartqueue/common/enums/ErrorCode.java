package com.smartqueue.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Auth errors
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_001", "Invalid email or password"),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH_002", "User with this email already exists"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_003", "Token has expired"),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "AUTH_004", "Token is invalid"),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "AUTH_005", "Refresh token has expired"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH_006", "Access denied"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH_007", "Insufficient permissions"),

    // Queue errors
    SERVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "QUEUE_001", "Queue service not found"),
    TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "QUEUE_002", "Token not found"),
    QUEUE_CLOSED(HttpStatus.BAD_REQUEST, "QUEUE_003", "Queue is currently closed"),
    ACTIVE_TOKEN_EXISTS(HttpStatus.CONFLICT, "QUEUE_004", "User already has an active token for this service"),
    INVALID_TOKEN_OPERATION(HttpStatus.BAD_REQUEST, "QUEUE_005", "Invalid operation for current token status"),

    // General errors
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "GEN_001", "Resource not found"),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "GEN_002", "Validation failed"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GEN_003", "An unexpected error occurred"),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "GEN_004", "Rate limit exceeded");

    private final HttpStatus httpStatus;
    private final String code;
    private final String defaultMessage;
}
