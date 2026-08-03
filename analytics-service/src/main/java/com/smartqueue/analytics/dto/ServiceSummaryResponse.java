package com.smartqueue.analytics.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for service summary.
 */
@Data
@Builder
public class ServiceSummaryResponse {
    private Long serviceId;
    private String serviceName;
    private long currentWaiting;
    private long todayTotal;
    private long todayCompleted;
    private double todayAvgWaitSeconds;
    private long weeklyTotal;
    private String peakDay;
}
