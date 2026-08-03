package com.smartqueue.queue.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CallNextResponse {
    private String calledTokenNumber;
    private Long userId;
    private String counterName;
    private int queueRemaining;
}
