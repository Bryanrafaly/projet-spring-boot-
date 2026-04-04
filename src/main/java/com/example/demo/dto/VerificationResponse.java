package com.example.demo.dto;

public record VerificationResponse(
        boolean valid,
        String uniqueIdentifier,
        String citizenNationalNumber,
        String citizenFullName,
        String status
) {
}
