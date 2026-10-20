package dev.as_request.dto;

import java.time.LocalDateTime;

public record CreateRepairRequest(
        Long productId,
        String customerName,
        String phone,
        String region,
        LocalDateTime visitDateTime,
        String memo
) {
}
