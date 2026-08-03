package com.smartqueue.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class UserProfileResponse {
    private Long id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String phone;
    private String profilePicture;
    private List<String> roles;
    private Instant createdAt;
    private boolean emailVerified;
}
