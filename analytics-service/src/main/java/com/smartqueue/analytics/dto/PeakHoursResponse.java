package com.smartqueue.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * DTO for peak hours statistics.
 */
@Data
@Builder
public class PeakHoursResponse {
    private Long serviceId;
    private List<HourlyBucket> data;

    @Data
    @Builder
    public static class HourlyBucket {
        private int dayOfWeek;
        private String dayName;
        private int hour;
        private double avgTokens;
        private double avgWaitSeconds;
    }
}
