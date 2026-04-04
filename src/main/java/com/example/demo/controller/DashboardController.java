package com.example.demo.controller;

import com.example.demo.dto.DashboardResponse;
import com.example.demo.model.Citizen;
import com.example.demo.model.IdentityCardRequest;
import com.example.demo.model.RequestStatus;
import com.example.demo.repository.CitizenRepository;
import com.example.demo.repository.IdentityCardRequestRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final CitizenRepository citizenRepository;
    private final IdentityCardRequestRepository requestRepository;
    private final UserRepository userRepository;

    public DashboardController(
            CitizenRepository citizenRepository,
            IdentityCardRequestRepository requestRepository,
            UserRepository userRepository) {
        this.citizenRepository = citizenRepository;
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    public DashboardResponse getDashboard() {
        List<IdentityCardRequest> requests = requestRepository.findAll();
        long overdueRequests = requests.stream()
                .filter(request -> request.getStatus() == RequestStatus.PENDING || request.getStatus() == RequestStatus.IN_PROGRESS)
                .filter(request -> request.getSubmissionDate().isBefore(LocalDateTime.now().minusDays(30)))
                .count();

        Map<String, Long> requestsByStatus = requests.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        request -> request.getStatus().name(),
                        java.util.stream.Collectors.counting()
                ));

        Map<String, Long> monthlyRequests = requests.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        request -> YearMonth.from(request.getSubmissionDate()).toString(),
                        java.util.stream.Collectors.counting()
                ))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), LinkedHashMap::putAll);

        Map<String, Long> citizensByRegion = citizenRepository.findByArchivedFalse().stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        Citizen::getRegion,
                        java.util.stream.Collectors.counting()
                ));

        return new DashboardResponse(
                citizenRepository.countByArchivedFalse(),
                requestRepository.countByStatus(RequestStatus.PRINTED),
                requestRepository.countByStatus(RequestStatus.PENDING),
                requestRepository.countByStatus(RequestStatus.VALIDATED),
                userRepository.countByActiveTrue(),
                overdueRequests,
                requestsByStatus,
                monthlyRequests,
                citizensByRegion
        );
    }
}
