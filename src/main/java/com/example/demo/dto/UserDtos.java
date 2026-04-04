package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public final class UserDtos {

    private UserDtos() {
    }

    public record UserRequest(
            @NotBlank
            String firstName,
            @NotBlank
            String lastName,
            @NotBlank
            @Email
            String email,
            String password,
            String role,
            Boolean active
    ) {
    }

    public record UserResponse(
            Long id,
            String firstName,
            String lastName,
            String email,
            String role,
            boolean active,
            LocalDateTime createdAt
    ) {
    }
}
