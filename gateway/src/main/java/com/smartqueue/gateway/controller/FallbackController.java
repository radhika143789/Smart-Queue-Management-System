package com.smartqueue.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    public record ApiResponse<T>(boolean success, ErrorResponse error) {}
    public record ErrorResponse(String code, String message) {}

    @GetMapping("/auth")
    public Mono<ResponseEntity<ApiResponse<Void>>> authFallback() {
        return createFallbackResponse("Auth service temporarily unavailable");
    }

    @GetMapping("/queue")
    public Mono<ResponseEntity<ApiResponse<Void>>> queueFallback() {
        return createFallbackResponse("Queue service temporarily unavailable");
    }

    @GetMapping("/admin")
    public Mono<ResponseEntity<ApiResponse<Void>>> adminFallback() {
        return createFallbackResponse("Admin service temporarily unavailable");
    }

    @GetMapping("/analytics")
    public Mono<ResponseEntity<ApiResponse<Void>>> analyticsFallback() {
        return createFallbackResponse("Analytics service temporarily unavailable");
    }

    @GetMapping("/notification")
    public Mono<ResponseEntity<ApiResponse<Void>>> notificationFallback() {
        return createFallbackResponse("Notification service temporarily unavailable");
    }

    private Mono<ResponseEntity<ApiResponse<Void>>> createFallbackResponse(String message) {
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ApiResponse<>(false, new ErrorResponse("SERVICE_UNAVAILABLE", message))));
    }
}
