package com.smartqueue.admin.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class AdminDashboardResponse {
    private int totalActiveServices;
    private long totalTokensToday;
    private long totalUsersRegistered;
    private List<String> systemAlerts;
    private List<AuditLogResponse> recentActivity;
}
