package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_sessions")
public class OtpSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String challengeId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String secret;

    @Column(nullable = false)
    private LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

    public OtpSession() {
    }

    public OtpSession(
            Long id,
            String challengeId,
            String email,
            String secret,
            LocalDateTime expiresAt) {
        this.id = id;
        this.challengeId = challengeId;
        this.email = email;
        this.secret = secret;
        this.expiresAt = expiresAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public static class Builder {
        private Long id;
        private String challengeId;
        private String email;
        private String secret;
        private LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(5);

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder challengeId(String challengeId) {
            this.challengeId = challengeId;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder secret(String secret) {
            this.secret = secret;
            return this;
        }

        public Builder expiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public OtpSession build() {
            return new OtpSession(id, challengeId, email, secret, expiresAt);
        }
    }
}
