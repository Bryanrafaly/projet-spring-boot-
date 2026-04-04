package com.example.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "identity_card_requests")
public class IdentityCardRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String fileNumber;

    @Column(nullable = false)
    private LocalDateTime submissionDate = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id", nullable = false)
    private Citizen citizen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private User agentResponsible;

    @Column(nullable = false, unique = true)
    private String qrCode;

    @Column(nullable = false, unique = true)
    private String uniqueIdentifier;

    public IdentityCardRequest() {
    }

    public IdentityCardRequest(
            Long id,
            String fileNumber,
            LocalDateTime submissionDate,
            RequestType requestType,
            RequestStatus status,
            Citizen citizen,
            User agentResponsible,
            String qrCode,
            String uniqueIdentifier) {
        this.id = id;
        this.fileNumber = fileNumber;
        this.submissionDate = submissionDate;
        this.requestType = requestType;
        this.status = status;
        this.citizen = citizen;
        this.agentResponsible = agentResponsible;
        this.qrCode = qrCode;
        this.uniqueIdentifier = uniqueIdentifier;
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

    public String getFileNumber() {
        return fileNumber;
    }

    public void setFileNumber(String fileNumber) {
        this.fileNumber = fileNumber;
    }

    public LocalDateTime getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(LocalDateTime submissionDate) {
        this.submissionDate = submissionDate;
    }

    public RequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(RequestType requestType) {
        this.requestType = requestType;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public Citizen getCitizen() {
        return citizen;
    }

    public void setCitizen(Citizen citizen) {
        this.citizen = citizen;
    }

    public User getAgentResponsible() {
        return agentResponsible;
    }

    public void setAgentResponsible(User agentResponsible) {
        this.agentResponsible = agentResponsible;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getUniqueIdentifier() {
        return uniqueIdentifier;
    }

    public void setUniqueIdentifier(String uniqueIdentifier) {
        this.uniqueIdentifier = uniqueIdentifier;
    }

    public static class Builder {
        private Long id;
        private String fileNumber;
        private LocalDateTime submissionDate = LocalDateTime.now();
        private RequestType requestType;
        private RequestStatus status = RequestStatus.PENDING;
        private Citizen citizen;
        private User agentResponsible;
        private String qrCode;
        private String uniqueIdentifier;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder fileNumber(String fileNumber) {
            this.fileNumber = fileNumber;
            return this;
        }

        public Builder submissionDate(LocalDateTime submissionDate) {
            this.submissionDate = submissionDate;
            return this;
        }

        public Builder requestType(RequestType requestType) {
            this.requestType = requestType;
            return this;
        }

        public Builder status(RequestStatus status) {
            this.status = status;
            return this;
        }

        public Builder citizen(Citizen citizen) {
            this.citizen = citizen;
            return this;
        }

        public Builder agentResponsible(User agentResponsible) {
            this.agentResponsible = agentResponsible;
            return this;
        }

        public Builder qrCode(String qrCode) {
            this.qrCode = qrCode;
            return this;
        }

        public Builder uniqueIdentifier(String uniqueIdentifier) {
            this.uniqueIdentifier = uniqueIdentifier;
            return this;
        }

        public IdentityCardRequest build() {
            return new IdentityCardRequest(
                    id,
                    fileNumber,
                    submissionDate,
                    requestType,
                    status,
                    citizen,
                    agentResponsible,
                    qrCode,
                    uniqueIdentifier
            );
        }
    }
}
