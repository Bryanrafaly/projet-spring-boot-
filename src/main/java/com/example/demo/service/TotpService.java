package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class TotpService {

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateOtpCode() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    public boolean isCodeValid(String expectedOtpCode, String providedOtpCode) {
        return expectedOtpCode != null
                && providedOtpCode != null
                && expectedOtpCode.equals(providedOtpCode.trim());
    }
}
