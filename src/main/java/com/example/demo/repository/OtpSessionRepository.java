package com.example.demo.repository;

import com.example.demo.model.OtpSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpSessionRepository extends JpaRepository<OtpSession, Long> {

    Optional<OtpSession> findByChallengeId(String challengeId);
}
