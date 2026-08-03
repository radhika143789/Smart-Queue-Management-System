package com.smartqueue.analytics.projection;

/**
 * Projection interface for native queries returning peak hour statistics.
 */
public interface PeakHourProjection {
    Long getServiceId();
    Integer getDayOfWeek();
    Integer getHourOfDay();
    Double getAvgTokens();
    Double getAvgWaitSeconds();
}
