package com.smartqueue.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SystemSettingRequest {
    @NotBlank
    private String settingKey;

    @NotBlank
    private String settingValue;

    private String description;

    @NotBlank
    private String category;
}
