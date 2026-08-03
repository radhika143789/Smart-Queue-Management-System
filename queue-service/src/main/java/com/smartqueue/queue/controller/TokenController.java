package com.smartqueue.queue.controller;

import com.smartqueue.common.dto.ApiResponse;
import com.smartqueue.queue.dto.BookTokenRequest;
import com.smartqueue.queue.dto.QueueStatusResponse;
import com.smartqueue.queue.dto.TokenResponse;
import com.smartqueue.queue.service.QueueService;
import com.smartqueue.queue.service.SseEmitterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class TokenController {

    private final QueueService queueService;
    private final SseEmitterService sseEmitterService;

    @PostMapping("/queues/{serviceId}/book")
    public ResponseEntity<ApiResponse<TokenResponse>> bookToken(
            @PathVariable Long serviceId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail,
            @RequestBody BookTokenRequest request) {
        
        TokenResponse response = queueService.bookToken(serviceId, userId, userEmail, request.getUserPhone());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Token booked successfully"));
    }

    @GetMapping("/queues/{serviceId}/status")
    public ResponseEntity<ApiResponse<QueueStatusResponse>> getQueueStatus(
            @PathVariable Long serviceId,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        
        QueueStatusResponse response = queueService.getQueueStatus(serviceId, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Queue status fetched"));
    }

    @GetMapping("/queues/{serviceId}/current")
    public ResponseEntity<ApiResponse<String>> getCurrentToken(
            @PathVariable Long serviceId) {
        QueueStatusResponse response = queueService.getQueueStatus(serviceId, null);
        return ResponseEntity.ok(ApiResponse.success(response.getCurrentlyServing(), "Current token fetched"));
    }

    @GetMapping(value = "/queues/{serviceId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamQueueUpdates(@PathVariable Long serviceId) {
        return sseEmitterService.subscribe(serviceId);
    }

    @GetMapping("/tokens/{tokenId}")
    public ResponseEntity<ApiResponse<TokenResponse>> getTokenDetails(
            @PathVariable Long tokenId,
            @RequestHeader("X-User-Id") Long userId) {
        
        TokenResponse response = queueService.getTokenDetails(tokenId, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Token details fetched"));
    }

    @PutMapping("/tokens/{tokenId}/cancel")
    public ResponseEntity<ApiResponse<TokenResponse>> cancelToken(
            @PathVariable Long tokenId,
            @RequestHeader("X-User-Id") Long userId) {
        
        TokenResponse response = queueService.cancelToken(tokenId, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Token cancelled"));
    }
}
