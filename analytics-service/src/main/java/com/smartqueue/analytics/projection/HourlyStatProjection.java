package com.smartqueue.analytics.projection;

/**
 * Projection interface for native queries returning hourly statistics.
 */
public interface HourlyStatProjection {
    Long getServiceId();
    Integer getHour();
    Long getTokensCompleted();
    Long getTokensNoShow();
    Long getTokensCancelled();
    Double getAvgWaitSeconds();
}
