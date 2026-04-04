package com.example.demo.controller;

import com.example.demo.dto.VerificationResponse;
import com.example.demo.model.IdentityCardRequest;
import com.example.demo.repository.IdentityCardRequestRepository;
import com.example.demo.service.QrCodeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/verify")
public class VerificationController {

    private final IdentityCardRequestRepository requestRepository;
    private final QrCodeService qrCodeService;

    public VerificationController(
            IdentityCardRequestRepository requestRepository,
            QrCodeService qrCodeService) {
        this.requestRepository = requestRepository;
        this.qrCodeService = qrCodeService;
    }

    @GetMapping("/{qrCode:.+}")
    public VerificationResponse verify(@PathVariable("qrCode") String qrCode) {
        if (!qrCodeService.isValid(qrCode)) {
            return new VerificationResponse(false, null, null, null, "INVALID_QR_CODE");
        }

        IdentityCardRequest request = requestRepository.findByQrCode(qrCode).orElse(null);
        if (request == null) {
            return new VerificationResponse(false, null, null, null, "REQUEST_NOT_FOUND");
        }

        return new VerificationResponse(
                true,
                request.getUniqueIdentifier(),
                request.getCitizen().getNationalNumber(),
                request.getCitizen().getFirstName() + " " + request.getCitizen().getLastName(),
                request.getStatus().name()
        );
    }
}
