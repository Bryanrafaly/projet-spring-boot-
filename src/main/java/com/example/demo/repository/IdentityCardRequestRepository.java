package com.example.demo.repository;

import com.example.demo.model.IdentityCardRequest;
import com.example.demo.model.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdentityCardRequestRepository extends JpaRepository<IdentityCardRequest, Long> {

    long countByStatus(RequestStatus status);

    Optional<IdentityCardRequest> findByQrCode(String qrCode);
}
