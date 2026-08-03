package com.smartqueue.queue.dto;

import com.smartqueue.common.enums.TokenStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class TokenResponse {
    private Long tokenId;
    private String tokenNumber;
    private String serviceName;
    private TokenStatus status;
    private int queuePosition;
    private int estimatedWaitSeconds;
    private String estimatedWaitDisplay;
    private String counterName;
    private Instant bookedAt;
    private Instant calledAt;
}
