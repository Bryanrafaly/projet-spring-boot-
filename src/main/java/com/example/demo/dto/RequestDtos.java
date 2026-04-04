package com.example.demo.dto;

import com.example.demo.model.RequestStatus;
import com.example.demo.model.RequestType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public final class RequestDtos {

    private RequestDtos() {
    }

    public record CreateCardRequest(
            @NotNull
            Long citizenId,
            Long agentResponsibleId,
            @NotNull
            RequestType requestType
    ) {
    }

    public record UpdateStatusRequest(
            @NotNull
            RequestStatus status
    ) {
    }

    public record CardRequestResponse(
            Long id,
            String fileNumber,
            LocalDateTime submissionDate,
            RequestType requestType,
            RequestStatus status,
            Long citizenId,
            Long agentResponsibleId,
            String qrCode,
            String uniqueIdentifier
    ) {
    }
}
