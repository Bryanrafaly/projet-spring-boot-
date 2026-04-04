package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank
            String firstName,
            @NotBlank
            String lastName,
            @NotBlank
            @Email
            String email,
            @NotBlank
            String password,
            String role
    ) {
    }

    public record LoginRequest(
            @NotBlank
            @Email
            String email,
            @NotBlank
            String password
    ) {
    }

    public record LoginResponse(
            String challengeId,
            String message,
            long expiresInSeconds,
            boolean emailSent,
            String debugOtpCode
    ) {
    }

    public record VerifyOtpRequest(
            @NotBlank
            String challengeId,
            @NotBlank
            String otpCode
    ) {
    }

    public record TokenResponse(
            String token
    ) {
    }

    public record ForgotPasswordRequest(
            @NotBlank
            @Email
            String email
    ) {
    }

    public record ForgotPasswordResponse(
            String challengeId,
            String message,
            long expiresInSeconds,
            boolean emailSent,
            String debugOtpCode
    ) {
    }

    public record ResetPasswordRequest(
            @NotBlank
            String challengeId,
            @NotBlank
            String otpCode,
            @NotBlank
            String newPassword
    ) {
    }

    public record MeResponse(
            Long id,
            String firstName,
            String lastName,
            String email,
            String role,
            boolean active
    ) {
    }
}
