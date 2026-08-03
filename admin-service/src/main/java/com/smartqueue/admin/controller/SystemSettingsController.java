package com.smartqueue.admin.controller;

import com.smartqueue.admin.dto.SystemSettingRequest;
import com.smartqueue.admin.entity.SystemSettingEntity;
import com.smartqueue.admin.service.AdminService;
import com.smartqueue.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/settings")
@RequiredArgsConstructor
@Slf4j
public class SystemSettingsController {

    private final AdminService adminService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SystemSettingEntity>>> getAllSettings() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllSettings(), "Settings retrieved"));
    }

    @GetMapping("/{key}")
    public ResponseEntity<ApiResponse<String>> getSetting(@PathVariable String key) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getSystemSetting(key), "Setting retrieved"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SystemSettingEntity>> createOrUpdateSetting(
            @Valid @RequestBody SystemSettingRequest request,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        SystemSettingEntity entity = adminService.updateSystemSetting(request, userId);
        return ResponseEntity.ok(ApiResponse.success(entity, "Setting saved successfully"));
    }
}
