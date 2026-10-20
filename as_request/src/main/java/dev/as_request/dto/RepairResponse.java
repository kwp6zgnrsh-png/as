package dev.as_request.dto;

import java.time.LocalDateTime;

public record RepairResponse(
        Long requestId,
        String requestNo,
        String customerName,
        String phone,
        Long productId,
        String region,
        String symptom,
        Long technicianId,
        String status,
        LocalDateTime createdAt
) {
}
