package dev.as_request.dto;

import java.time.LocalDateTime;

public record RepairListResponse(
	String requestNo,
	String customerName,
	String phone,
	String region,
	String symptom,
	String status,
	LocalDateTime createdAt
) {
}