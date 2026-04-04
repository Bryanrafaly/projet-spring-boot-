package com.example.demo.controller;

import com.example.demo.dto.AuthDtos.ForgotPasswordRequest;
import com.example.demo.dto.AuthDtos.ForgotPasswordResponse;
import com.example.demo.dto.AuthDtos.LoginRequest;
import com.example.demo.dto.AuthDtos.LoginResponse;
import com.example.demo.dto.AuthDtos.MeResponse;
import com.example.demo.dto.AuthDtos.RegisterRequest;
import com.example.demo.dto.AuthDtos.ResetPasswordRequest;
import com.example.demo.dto.AuthDtos.TokenResponse;
import com.example.demo.dto.AuthDtos.VerifyOtpRequest;
import com.example.demo.model.OtpSession;
import com.example.demo.model.User;
import com.example.demo.repository.OtpSessionRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtService;
import com.example.demo.service.EmailOtpService;
import com.example.demo.service.TotpService;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final UserService userService;
    private final TotpService totpService;
    private final EmailOtpService emailOtpService;
    private final OtpSessionRepository otpSessionRepository;

    public AuthController(
            UserRepository userRepository,
            JwtService jwtService,
            UserService userService,
            TotpService totpService,
            EmailOtpService emailOtpService,
            OtpSessionRepository otpSessionRepository) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.userService = userService;
        this.totpService = totpService;
        this.emailOtpService = emailOtpService;
        this.otpSessionRepository = otpSessionRepository;
    }

    @PostMapping("/register")
    public String register(@Valid @RequestBody RegisterRequest request) {
        userService.createUser(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.password(),
                request.role()
        );
        return "User registered successfully";
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        User user = userRepository
                .findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        if (!user.isActive()) {
            throw new RuntimeException("Inactive account");
        }

        if (!userService.passwordMatches(
                request.password(),
                user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        String otpCode = totpService.generateOtpCode();
        String challengeId = UUID.randomUUID().toString();

        otpSessionRepository.save(OtpSession.builder()
                .challengeId(challengeId)
                .email(user.getEmail())
                .secret(otpCode)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build());

        boolean emailSent = emailOtpService.sendOtpCode(user.getEmail(), otpCode);

        return new LoginResponse(
                challengeId,
                emailSent
                        ? "Un code OTP a ete envoye a votre adresse e-mail."
                        : "E-mail OTP indisponible. Utilisez debugOtpCode pour les tests locaux.",
                300,
                emailSent,
                emailSent ? null : otpCode
        );
    }

    @PostMapping("/verify-otp")
    public TokenResponse verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        OtpSession otpSession = otpSessionRepository.findByChallengeId(request.challengeId())
                .orElseThrow(() -> new RuntimeException("Challenge not found"));

        if (otpSession.getExpiresAt().isBefore(LocalDateTime.now())) {
            otpSessionRepository.delete(otpSession);
            throw new RuntimeException("Challenge expired");
        }

        if (!totpService.isCodeValid(otpSession.getSecret(), request.otpCode())) {
            throw new RuntimeException("Invalid OTP code");
        }

        otpSessionRepository.delete(otpSession);
        return new TokenResponse(jwtService.generateToken(otpSession.getEmail()));
    }

    @PostMapping("/logout")
    public String logout() {
        return "Logout successful";
    }

    @PostMapping("/forgot-password")
    public ForgotPasswordResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(request.email());
        if (userOptional.isEmpty()) {
            return new ForgotPasswordResponse(
                    null,
                    "Aucun compte trouve avec cet e-mail.",
                    300,
                    false,
                    null
            );
        }
        User user = userOptional.get();

        String otpCode = totpService.generateOtpCode();
        String challengeId = UUID.randomUUID().toString();

        otpSessionRepository.save(OtpSession.builder()
                .challengeId(challengeId)
                .email(user.getEmail())
                .secret(otpCode)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build());

        boolean emailSent = emailOtpService.sendPasswordResetCode(user.getEmail(), otpCode);

        return new ForgotPasswordResponse(
                challengeId,
                emailSent
                        ? "Un code de reinitialisation a ete envoye a votre adresse e-mail."
                        : "E-mail indisponible. Utilisez debugOtpCode pour les tests locaux.",
                300,
                emailSent,
                emailSent ? null : otpCode
        );
    }

    @PostMapping("/reset-password")
    public String resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        OtpSession otpSession = otpSessionRepository.findByChallengeId(request.challengeId())
                .orElseThrow(() -> new RuntimeException("Challenge not found"));

        if (otpSession.getExpiresAt().isBefore(LocalDateTime.now())) {
            otpSessionRepository.delete(otpSession);
            throw new RuntimeException("Challenge expired");
        }

        if (!totpService.isCodeValid(otpSession.getSecret(), request.otpCode())) {
            throw new RuntimeException("Invalid OTP code");
        }

        userService.updatePasswordByEmail(otpSession.getEmail(), request.newPassword());
        otpSessionRepository.delete(otpSession);
        return "Password reset successful";
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public MeResponse me(Principal principal) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        return new MeResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole().name(),
                user.isActive()
        );
    }
}
