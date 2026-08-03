package com.smartqueue.queue.controller;

import com.smartqueue.common.dto.ApiResponse;
import com.smartqueue.queue.dto.CallNextResponse;
import com.smartqueue.queue.dto.QueueStatusResponse;
import com.smartqueue.queue.dto.TokenResponse;
import com.smartqueue.queue.service.QueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminQueueController {

    private final QueueService queueService;

    @PostMapping("/queues/{serviceId}/call-next")
    public ResponseEntity<ApiResponse<CallNextResponse>> callNextToken(
            @PathVariable Long serviceId,
            @RequestParam Long counterId,
            @RequestHeader("X-User-Id") Long staffUserId) {
        
        CallNextResponse response = queueService.callNextToken(serviceId, counterId, staffUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Next token called"));
    }

    @PostMapping("/queues/{serviceId}/pause")
    public ResponseEntity<ApiResponse<Void>> pauseQueue(@PathVariable Long serviceId) {
        // Implement logic to pause queue (e.g., set isActive = false on ServiceEntity temporarily)
        return ResponseEntity.ok(ApiResponse.success(null, "Queue paused"));
    }

    @PostMapping("/queues/{serviceId}/resume")
    public ResponseEntity<ApiResponse<Void>> resumeQueue(@PathVariable Long serviceId) {
        // Implement logic to resume queue
        return ResponseEntity.ok(ApiResponse.success(null, "Queue resumed"));
    }

    @PostMapping("/tokens/{tokenId}/complete")
    public ResponseEntity<ApiResponse<TokenResponse>> completeToken(
            @PathVariable Long tokenId,
            @RequestHeader("X-User-Id") Long staffUserId) {
        
        TokenResponse response = queueService.completeToken(tokenId, staffUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Token marked as completed"));
    }

    @PostMapping("/tokens/{tokenId}/no-show")
    public ResponseEntity<ApiResponse<TokenResponse>> markNoShow(
            @PathVariable Long tokenId,
            @RequestHeader("X-User-Id") Long staffUserId) {
        
        TokenResponse response = queueService.markNoShow(tokenId);
        return ResponseEntity.ok(ApiResponse.success(response, "Token marked as no-show"));
    }

    @GetMapping("/queues/{serviceId}/dashboard")
    public ResponseEntity<ApiResponse<QueueStatusResponse>> getQueueDashboard(
            @PathVariable Long serviceId) {
        
        QueueStatusResponse response = queueService.getQueueStatus(serviceId, null);
        return ResponseEntity.ok(ApiResponse.success(response, "Dashboard info fetched"));
    }
}
