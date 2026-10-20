package dev.as_request.dto;

public record TechnicianResponse(
	Long technicianId,
	String technicianName,
	String region,
	boolean active
) {}