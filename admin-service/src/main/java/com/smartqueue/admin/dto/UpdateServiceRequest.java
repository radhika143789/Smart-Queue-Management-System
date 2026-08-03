package com.smartqueue.admin.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateServiceRequest {
    @Size(max = 100)
    private String name;

    private String description;
    private String location;

    @Min(1)
    @Max(10000)
    private Integer maxDailyTokens;

    @Min(30)
    @Max(3600)
    private Integer avgServiceTimeSeconds;

    @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$")
    private String openTime;

    @Pattern(regexp = "^([01]\\d|2[0-3]):([0-5]\\d)$")
    private String closeTime;

    private Boolean isActive;
}
