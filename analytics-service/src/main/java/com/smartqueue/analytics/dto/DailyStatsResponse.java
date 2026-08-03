package com.smartqueue.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * DTO for daily statistics.
 */
@Data
@Builder
public class DailyStatsResponse {
    private Long serviceId;
    private String serviceName;
    private LocalDate date;
    private long totalTokens;
    private long tokensCompleted;
    private long tokensCancelled;
    private long tokensNoShow;
    private double completionRate;
    private double avgWaitSeconds;
    private String avgWaitDisplay;
    private int peakHour;
    private long peakHourTokens;
}
