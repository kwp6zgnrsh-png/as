package dev.as_request.dto;

import java.time.LocalDateTime;

public record AssignmentResponse(
	Long assignmentId,
	Long requestId,
	Long technicianId,
	String status,
	LocalDateTime assignedAt
) {
}