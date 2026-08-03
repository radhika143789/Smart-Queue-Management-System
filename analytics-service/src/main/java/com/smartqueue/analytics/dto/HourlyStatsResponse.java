package com.smartqueue.analytics.dto;

import lombok.Builder;
import lombok.Data;

/**
 * DTO for hourly statistics.
 */
@Data
@Builder
public class HourlyStatsResponse {
    private Long serviceId;
    private int hour;
    private long tokensCompleted;
    private long tokensNoShow;
    private long tokensCancelled;
    private double avgWaitSeconds;
}
