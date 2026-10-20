package dev.as_request.dto;

import java.time.LocalDateTime;

public record RepairResponse(
        Long requestId,
        Long productId,
        String customerName,
        String phone,
        String region,
        LocalDateTime visitDateTime,
        String memo,
        String status,
        LocalDateTime createdAt
) {
}
