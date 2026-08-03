package com.smartqueue.queue.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class BookTokenRequest {
    @NotNull(message = "Service ID is required")
    private Long serviceId;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number")
    private String userPhone;
}
