package com.smartqueue.admin.controller;

import com.smartqueue.admin.dto.AdminDashboardResponse;
import com.smartqueue.admin.dto.AuditLogResponse;
import com.smartqueue.admin.service.AdminService;
import com.smartqueue.common.dto.ApiResponse;
import com.smartqueue.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Slf4j
public class AdminDashboardController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Email", required = false) String email) {
        log.info("Admin dashboard requested by user: {}", userId);
        AdminDashboardResponse dashboard = adminService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success(dashboard, "Dashboard data retrieved successfully"));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        log.info("Audit logs requested by user: {}", userId);
        PageResponse<AuditLogResponse> logs = adminService.getAuditLogs(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt")));
        return ResponseEntity.ok(ApiResponse.success(logs, "Audit logs retrieved successfully"));
    }
}
