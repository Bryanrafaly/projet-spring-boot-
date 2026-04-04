package com.example.demo.dto;

import com.example.demo.model.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

public final class CitizenDtos {

    private CitizenDtos() {
    }

    public record CitizenRequest(
            @NotBlank
            String nationalNumber,
            @NotBlank
            String firstName,
            @NotBlank
            String lastName,
            @NotNull
            LocalDate birthDate,
            @NotBlank
            String birthPlace,
            @NotNull
            Gender gender,
            @NotBlank
            String address,
            @NotBlank
            String region,
            String profession,
            String photo
    ) {
    }

    public record CitizenResponse(
            Long id,
            String nationalNumber,
            String firstName,
            String lastName,
            LocalDate birthDate,
            String birthPlace,
            Gender gender,
            String address,
            String region,
            String profession,
            String photo,
            boolean archived,
            LocalDateTime createdAt
    ) {
    }
}
