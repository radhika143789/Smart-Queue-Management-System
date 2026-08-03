package com.smartqueue.queue.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QueueStatusResponse {
    private Long serviceId;
    private String serviceName;
    private boolean isOpen;
    private String currentlyServing;
    private int totalWaiting;
    private int estimatedWaitForNextSeconds;
    private TokenResponse myToken;
}
