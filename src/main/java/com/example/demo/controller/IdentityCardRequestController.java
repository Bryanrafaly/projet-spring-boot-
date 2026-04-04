package com.example.demo.controller;

import com.example.demo.dto.RequestDtos.CardRequestResponse;
import com.example.demo.dto.RequestDtos.CreateCardRequest;
import com.example.demo.dto.RequestDtos.UpdateStatusRequest;
import com.example.demo.model.Citizen;
import com.example.demo.model.IdentityCardRequest;
import com.example.demo.model.RequestStatus;
import com.example.demo.model.User;
import com.example.demo.repository.CitizenRepository;
import com.example.demo.repository.IdentityCardRequestRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.QrCodeService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/requests")
public class IdentityCardRequestController {

    private final IdentityCardRequestRepository requestRepository;
    private final CitizenRepository citizenRepository;
    private final UserRepository userRepository;
    private final QrCodeService qrCodeService;

    public IdentityCardRequestController(
            IdentityCardRequestRepository requestRepository,
            CitizenRepository citizenRepository,
            UserRepository userRepository,
            QrCodeService qrCodeService) {
        this.requestRepository = requestRepository;
        this.citizenRepository = citizenRepository;
        this.userRepository = userRepository;
        this.qrCodeService = qrCodeService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_REGISTRATION')")
    public CardRequestResponse create(@Valid @RequestBody CreateCardRequest request) {
        Citizen citizen = citizenRepository.findById(request.citizenId())
                .orElseThrow(() -> new IllegalArgumentException("Citizen not found for id " + request.citizenId()));
        if (citizen.isArchived()) {
            throw new IllegalArgumentException("Archived citizen cannot submit a request");
        }

        User agent = request.agentResponsibleId() == null
                ? null
                : userRepository.findById(request.agentResponsibleId())
                .orElseThrow(() -> new IllegalArgumentException("Agent not found for id " + request.agentResponsibleId()));
        if (agent != null && !agent.isActive()) {
            throw new IllegalArgumentException("Inactive agent cannot be assigned");
        }

        String uniqueIdentifier = UUID.randomUUID().toString();
        String qrCode = qrCodeService.generateSignedQrCode(uniqueIdentifier);

        IdentityCardRequest entity = IdentityCardRequest.builder()
                .fileNumber("DOS-" + System.currentTimeMillis())
                .requestType(request.requestType())
                .status(RequestStatus.PENDING)
                .citizen(citizen)
                .agentResponsible(agent)
                .uniqueIdentifier(uniqueIdentifier)
                .qrCode(qrCode)
                .build();

        return toResponse(requestRepository.save(entity));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_REGISTRATION', 'AGENT_VALIDATION', 'SUPERVISOR')")
    public List<CardRequestResponse> findAll() {
        return requestRepository.findAll().stream().map(this::toResponse).toList();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_VALIDATION', 'SUPERVISOR')")
    public CardRequestResponse updateStatus(@PathVariable("id") Long id, @Valid @RequestBody UpdateStatusRequest request) {
        IdentityCardRequest entity = requestRepository.findById(id).orElseThrow();
        entity.setStatus(request.status());
        return toResponse(requestRepository.save(entity));
    }

    private CardRequestResponse toResponse(IdentityCardRequest entity) {
        return new CardRequestResponse(
                entity.getId(),
                entity.getFileNumber(),
                entity.getSubmissionDate(),
                entity.getRequestType(),
                entity.getStatus(),
                entity.getCitizen().getId(),
                entity.getAgentResponsible() == null ? null : entity.getAgentResponsible().getId(),
                entity.getQrCode(),
                entity.getUniqueIdentifier()
        );
    }
}
