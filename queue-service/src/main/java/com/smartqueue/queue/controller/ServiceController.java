package com.smartqueue.queue.controller;

import com.smartqueue.common.dto.ApiResponse;
import com.smartqueue.queue.dto.ServiceResponse;
import com.smartqueue.queue.entity.ServiceEntity;
import com.smartqueue.queue.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceRepository serviceRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ServiceResponse>>> getAllActiveServices() {
        List<ServiceResponse> services = serviceRepository.findByIsActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(services, "Services fetched"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ServiceResponse>> getServiceDetails(@PathVariable Long id) {
        ServiceEntity service = serviceRepository.findByIdAndIsActiveTrue(id)
                .orElseThrow(() -> new RuntimeException("Service not found"));
        return ResponseEntity.ok(ApiResponse.success(mapToResponse(service), "Service fetched"));
    }

    private ServiceResponse mapToResponse(ServiceEntity service) {
        return ServiceResponse.builder()
                .id(service.getId())
                .name(service.getName())
                .description(service.getDescription())
                .location(service.getLocation())
                .isActive(service.isActive())
                .avgServiceTimeSeconds(service.getAvgServiceTimeSeconds())
                .openTime(service.getOpenTime())
                .closeTime(service.getCloseTime())
                .totalWaitingNow(0) // Need to calculate from redis or DB if needed here
                .build();
    }
}
