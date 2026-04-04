package com.example.demo.controller;

import com.example.demo.dto.CitizenDtos.CitizenRequest;
import com.example.demo.dto.CitizenDtos.CitizenResponse;
import com.example.demo.model.Citizen;
import com.example.demo.repository.CitizenRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/citizens")
public class CitizenController {

    private final CitizenRepository citizenRepository;

    public CitizenController(CitizenRepository citizenRepository) {
        this.citizenRepository = citizenRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_REGISTRATION')")
    public CitizenResponse create(@Valid @RequestBody CitizenRequest request) {
        citizenRepository.findByNationalNumber(request.nationalNumber()).ifPresent(existing -> {
            throw new IllegalArgumentException("A citizen with this national number already exists");
        });

        Citizen citizen = Citizen.builder()
                .nationalNumber(request.nationalNumber())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .birthDate(request.birthDate())
                .birthPlace(request.birthPlace())
                .gender(request.gender())
                .address(request.address())
                .region(request.region())
                .profession(request.profession())
                .photo(request.photo())
                .build();
        return toResponse(citizenRepository.save(citizen));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_REGISTRATION', 'AGENT_VALIDATION', 'SUPERVISOR')")
    public List<CitizenResponse> findAll(@RequestParam(name = "search", required = false) String search) {
        List<Citizen> citizens = (search == null || search.isBlank())
                ? citizenRepository.findByArchivedFalse()
                : citizenRepository.findByLastNameContainingIgnoreCaseOrNationalNumberContainingIgnoreCase(search, search);
        return citizens.stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_REGISTRATION', 'AGENT_VALIDATION', 'SUPERVISOR')")
    public CitizenResponse findById(@PathVariable("id") Long id) {
        return toResponse(citizenRepository.findById(id).orElseThrow());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT_REGISTRATION')")
    public CitizenResponse update(@PathVariable("id") Long id, @Valid @RequestBody CitizenRequest request) {
        Citizen citizen = citizenRepository.findById(id).orElseThrow();
        citizenRepository.findByNationalNumber(request.nationalNumber())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("A citizen with this national number already exists");
                });
        citizen.setNationalNumber(request.nationalNumber());
        citizen.setFirstName(request.firstName());
        citizen.setLastName(request.lastName());
        citizen.setBirthDate(request.birthDate());
        citizen.setBirthPlace(request.birthPlace());
        citizen.setGender(request.gender());
        citizen.setAddress(request.address());
        citizen.setRegion(request.region());
        citizen.setProfession(request.profession());
        citizen.setPhoto(request.photo());
        return toResponse(citizenRepository.save(citizen));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable("id") Long id) {
        Citizen citizen = citizenRepository.findById(id).orElseThrow();
        citizen.setArchived(true);
        citizenRepository.save(citizen);
    }

    private CitizenResponse toResponse(Citizen citizen) {
        return new CitizenResponse(
                citizen.getId(),
                citizen.getNationalNumber(),
                citizen.getFirstName(),
                citizen.getLastName(),
                citizen.getBirthDate(),
                citizen.getBirthPlace(),
                citizen.getGender(),
                citizen.getAddress(),
                citizen.getRegion(),
                citizen.getProfession(),
                citizen.getPhoto(),
                citizen.isArchived(),
                citizen.getCreatedAt()
        );
    }
}
