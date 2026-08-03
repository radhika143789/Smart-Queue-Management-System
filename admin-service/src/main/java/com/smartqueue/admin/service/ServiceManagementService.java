package com.smartqueue.admin.service;

import com.smartqueue.admin.dto.CreateServiceRequest;
import com.smartqueue.admin.dto.UpdateServiceRequest;
import com.smartqueue.admin.entity.ManagedServiceEntity;
import com.smartqueue.admin.repository.ManagedServiceRepository;
import com.smartqueue.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServiceManagementService {

    private final ManagedServiceRepository managedServiceRepository;
    private final AdminService adminService;

    @Transactional
    public ManagedServiceEntity createService(CreateServiceRequest request, Long actorId, String actorEmail, String ip) {
        ManagedServiceEntity entity = ManagedServiceEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .location(request.getLocation())
                .maxDailyTokens(request.getMaxDailyTokens())
                .avgServiceTimeSeconds(request.getAvgServiceTimeSeconds())
                .openTime(request.getOpenTime())
                .closeTime(request.getCloseTime())
                .createdBy(actorId)
                .isActive(true)
                .build();

        ManagedServiceEntity saved = managedServiceRepository.save(entity);

        adminService.createAuditLog(actorId, actorEmail, "SERVICE_CREATED", "SERVICE", saved.getId().toString(), "Created service: " + saved.getName(), ip);

        return saved;
    }

    @Transactional
    public ManagedServiceEntity updateService(Long serviceId, UpdateServiceRequest request, Long actorId, String actorEmail, String ip) {
        ManagedServiceEntity entity = managedServiceRepository.findById(serviceId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "SERVICE_NOT_FOUND", "Service not found"));

        if (request.getName() != null) entity.setName(request.getName());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getLocation() != null) entity.setLocation(request.getLocation());
        if (request.getMaxDailyTokens() != null) entity.setMaxDailyTokens(request.getMaxDailyTokens());
        if (request.getAvgServiceTimeSeconds() != null) entity.setAvgServiceTimeSeconds(request.getAvgServiceTimeSeconds());
        if (request.getOpenTime() != null) entity.setOpenTime(request.getOpenTime());
        if (request.getCloseTime() != null) entity.setCloseTime(request.getCloseTime());
        if (request.getIsActive() != null) entity.setActive(request.getIsActive());

        ManagedServiceEntity saved = managedServiceRepository.save(entity);

        adminService.createAuditLog(actorId, actorEmail, "SERVICE_UPDATED", "SERVICE", saved.getId().toString(), "Updated service details", ip);

        return saved;
    }

    @Transactional
    public void toggleService(Long serviceId, boolean active, Long actorId, String actorEmail, String ip) {
        ManagedServiceEntity entity = managedServiceRepository.findById(serviceId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "SERVICE_NOT_FOUND", "Service not found"));

        entity.setActive(active);
        managedServiceRepository.save(entity);

        String action = active ? "SERVICE_RESUMED" : "SERVICE_PAUSED";
        adminService.createAuditLog(actorId, actorEmail, action, "SERVICE", entity.getId().toString(), "Changed active status to " + active, ip);
    }

    public List<ManagedServiceEntity> getAllServices() {
        return managedServiceRepository.findAll();
    }

    public ManagedServiceEntity getServiceById(Long id) {
        return managedServiceRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "SERVICE_NOT_FOUND", "Service not found"));
    }
}
