package com.smartqueue.admin.controller;

import com.smartqueue.admin.dto.CreateServiceRequest;
import com.smartqueue.admin.dto.UpdateServiceRequest;
import com.smartqueue.admin.entity.ManagedServiceEntity;
import com.smartqueue.admin.service.ServiceManagementService;
import com.smartqueue.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/services")
@RequiredArgsConstructor
@Slf4j
public class ServiceManagementController {

    private final ServiceManagementService serviceManagementService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ManagedServiceEntity>>> getAllServices() {
        return ResponseEntity.ok(ApiResponse.success(serviceManagementService.getAllServices(), "Services retrieved"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ManagedServiceEntity>> getServiceById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(serviceManagementService.getServiceById(id), "Service retrieved"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ManagedServiceEntity>> createService(
            @Valid @RequestBody CreateServiceRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Email", required = false) String email,
            HttpServletRequest httpServletRequest) {
        String ip = httpServletRequest.getRemoteAddr();
        ManagedServiceEntity entity = serviceManagementService.createService(request, userId, email, ip);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(entity, "Service created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ManagedServiceEntity>> updateService(
            @PathVariable Long id,
            @Valid @RequestBody UpdateServiceRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Email", required = false) String email,
            HttpServletRequest httpServletRequest) {
        String ip = httpServletRequest.getRemoteAddr();
        ManagedServiceEntity entity = serviceManagementService.updateService(id, request, userId, email, ip);
        return ResponseEntity.ok(ApiResponse.success(entity, "Service updated successfully"));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<ApiResponse<Void>> pauseService(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Email", required = false) String email,
            HttpServletRequest httpServletRequest) {
        String ip = httpServletRequest.getRemoteAddr();
        serviceManagementService.toggleService(id, false, userId, email, ip);
        return ResponseEntity.ok(ApiResponse.success(null, "Service paused successfully"));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<ApiResponse<Void>> resumeService(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Email", required = false) String email,
            HttpServletRequest httpServletRequest) {
        String ip = httpServletRequest.getRemoteAddr();
        serviceManagementService.toggleService(id, true, userId, email, ip);
        return ResponseEntity.ok(ApiResponse.success(null, "Service resumed successfully"));
    }
}
