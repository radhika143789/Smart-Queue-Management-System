package com.smartqueue.admin.service;

import com.smartqueue.admin.dto.AdminDashboardResponse;
import com.smartqueue.admin.dto.AuditLogResponse;
import com.smartqueue.admin.dto.SystemSettingRequest;
import com.smartqueue.admin.entity.AuditLogEntity;
import com.smartqueue.admin.entity.SystemSettingEntity;
import com.smartqueue.admin.repository.AuditLogRepository;
import com.smartqueue.admin.repository.ManagedServiceRepository;
import com.smartqueue.admin.repository.SystemSettingRepository;
import com.smartqueue.common.dto.PageResponse;
import com.smartqueue.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminService {

    private final AuditLogRepository auditLogRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final ManagedServiceRepository managedServiceRepository;

    public AdminDashboardResponse getDashboard() {
        int activeServices = managedServiceRepository.findByIsActiveTrue().size();
        // Placeholders for real metrics
        long totalTokensToday = 150L; 
        long totalUsersRegistered = 2000L;
        
        Pageable top10 = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "occurredAt"));
        List<AuditLogResponse> recentActivity = auditLogRepository.findAll(top10)
                .stream()
                .map(this::mapToAuditLogResponse)
                .collect(Collectors.toList());

        return AdminDashboardResponse.builder()
                .totalActiveServices(activeServices)
                .totalTokensToday(totalTokensToday)
                .totalUsersRegistered(totalUsersRegistered)
                .systemAlerts(List.of())
                .recentActivity(recentActivity)
                .build();
    }

    @Transactional
    public void createAuditLog(Long actorId, String actorEmail, String action, String targetType, String targetId, String details, String ip) {
        AuditLogEntity logEntry = AuditLogEntity.builder()
                .actorUserId(actorId)
                .actorEmail(actorEmail)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .ipAddress(ip)
                .occurredAt(Instant.now())
                .build();
        auditLogRepository.save(logEntry);
    }

    public String getSystemSetting(String key) {
        return systemSettingRepository.findBySettingKey(key)
                .map(SystemSettingEntity::getSettingValue)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Setting not found"));
    }

    @Transactional
    public SystemSettingEntity updateSystemSetting(SystemSettingRequest request, Long updatedBy) {
        SystemSettingEntity entity = systemSettingRepository.findBySettingKey(request.getSettingKey())
                .orElseGet(() -> SystemSettingEntity.builder()
                        .settingKey(request.getSettingKey())
                        .build());
        
        entity.setSettingValue(request.getSettingValue());
        entity.setDescription(request.getDescription());
        entity.setCategory(request.getCategory());
        entity.setUpdatedBy(updatedBy);
        
        SystemSettingEntity saved = systemSettingRepository.save(entity);
        
        createAuditLog(updatedBy, null, "SETTING_UPDATED", "SETTING", request.getSettingKey(), "Updated value to " + request.getSettingValue(), null);
        
        return saved;
    }

    public List<SystemSettingEntity> getAllSettings() {
        return systemSettingRepository.findAll();
    }

    public PageResponse<AuditLogResponse> getAuditLogs(Pageable pageable) {
        Page<AuditLogEntity> page = auditLogRepository.findAll(pageable);
        List<AuditLogResponse> content = page.getContent().stream()
                .map(this::mapToAuditLogResponse)
                .collect(Collectors.toList());
        return PageResponse.<AuditLogResponse>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isLast(page.isLast())
                .build();
    }

    private AuditLogResponse mapToAuditLogResponse(AuditLogEntity entity) {
        return AuditLogResponse.builder()
                .id(entity.getId())
                .actorEmail(entity.getActorEmail())
                .action(entity.getAction())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .details(entity.getDetails())
                .ipAddress(entity.getIpAddress())
                .occurredAt(entity.getOccurredAt())
                .build();
    }
}
