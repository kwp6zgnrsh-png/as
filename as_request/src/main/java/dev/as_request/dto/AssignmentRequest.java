package dev.as_request.dto;

import java.time.LocalDateTime;

public record AssignmentRequest(
	Long requestId,
	Long technicianId,
	LocalDateTime date,
	String timeSlot
) {}