package com.smartqueue.admin.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class AuditLogResponse {
    private Long id;
    private String actorEmail;
    private String action;
    private String targetType;
    private String targetId;
    private String details;
    private String ipAddress;
    private Instant occurredAt;
}
