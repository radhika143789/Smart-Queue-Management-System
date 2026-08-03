package com.smartqueue.analytics.controller;

import com.smartqueue.analytics.dto.DailyStatsResponse;
import com.smartqueue.analytics.dto.HourlyStatsResponse;
import com.smartqueue.analytics.dto.PeakHoursResponse;
import com.smartqueue.analytics.dto.ServiceSummaryResponse;
import com.smartqueue.analytics.service.AnalyticsService;
import com.smartqueue.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for providing analytics data.
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Slf4j
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/services/{serviceId}/daily")
    public ResponseEntity<ApiResponse<DailyStatsResponse>> getDailyStats(
            @PathVariable Long serviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        if (date == null) {
            date = LocalDate.now();
        }
        DailyStatsResponse stats = analyticsService.getDailyStats(serviceId, date);
        return ResponseEntity.ok(ApiResponse.success("Daily stats retrieved successfully", stats));
    }

    @GetMapping("/services/{serviceId}/hourly")
    public ResponseEntity<ApiResponse<List<HourlyStatsResponse>>> getHourlyStats(
            @PathVariable Long serviceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        if (date == null) {
            date = LocalDate.now();
        }
        List<HourlyStatsResponse> stats = analyticsService.getHourlyBreakdown(serviceId, date);
        return ResponseEntity.ok(ApiResponse.success("Hourly stats retrieved successfully", stats));
    }

    @GetMapping("/services/{serviceId}/peak-hours")
    public ResponseEntity<ApiResponse<PeakHoursResponse>> getPeakHours(
            @PathVariable Long serviceId) {
        
        PeakHoursResponse stats = analyticsService.getPeakHours(serviceId);
        return ResponseEntity.ok(ApiResponse.success("Peak hours stats retrieved successfully", stats));
    }

    @GetMapping("/services/{serviceId}/summary")
    public ResponseEntity<ApiResponse<ServiceSummaryResponse>> getServiceSummary(
            @PathVariable Long serviceId,
            @RequestParam(required = false) String serviceName) {
        
        ServiceSummaryResponse summary = analyticsService.getServiceSummary(serviceId, serviceName);
        return ResponseEntity.ok(ApiResponse.success("Service summary retrieved successfully", summary));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Analytics Service is up and running", "OK"));
    }
}
