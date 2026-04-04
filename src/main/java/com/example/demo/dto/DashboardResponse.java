package com.example.demo.dto;

import java.util.Map;

public record DashboardResponse(
        long totalCitizens,
        long totalCardsDelivered,
        long pendingRequests,
        long validatedRequests,
        long activeUsers,
        long overdueRequests,
        Map<String, Long> requestsByStatus,
        Map<String, Long> monthlyRequests,
        Map<String, Long> citizensByRegion
) {
}
