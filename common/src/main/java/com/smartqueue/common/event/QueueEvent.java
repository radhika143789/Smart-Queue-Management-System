package com.smartqueue.common.event;

import com.smartqueue.common.enums.TokenStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Kafka event published when a queue token changes state.
 * This is the primary inter-service event for the SmartQueue system.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueEvent {
    private String eventId;
    private String eventType;       // TOKEN_BOOKED, TOKEN_CALLED, TOKEN_COMPLETED, TOKEN_CANCELLED
    private Long tokenId;
    private String tokenNumber;     // Display number e.g. "A-042"
    private Long serviceId;
    private String serviceName;
    private Long userId;
    private String userEmail;
    private String userPhone;
    private TokenStatus oldStatus;
    private TokenStatus newStatus;
    private Integer queuePosition;  // Position in queue at time of event
    private Integer estimatedWaitSeconds;
    private String counterName;     // For TOKEN_CALLED events
    private Instant occurredAt;
    private String tenantId;        // For multi-tenant support

    /** Derived convenience field — minutes rounded up from estimatedWaitSeconds */
    public int getEstimatedWaitMinutes() {
        if (estimatedWaitSeconds == null || estimatedWaitSeconds <= 0) return 0;
        return (int) Math.ceil(estimatedWaitSeconds / 60.0);
    }
}
