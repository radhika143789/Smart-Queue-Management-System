package com.smartqueue.queue.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServiceResponse {
    private Long id;
    private String name;
    private String description;
    private String location;
    private boolean isActive;
    private int avgServiceTimeSeconds;
    private String openTime;
    private String closeTime;
    private int totalWaitingNow;
}
