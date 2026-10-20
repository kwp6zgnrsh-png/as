package dev.as_request.service;

import dev.as_request.dto.AssignmentRequest;
import dev.as_request.dto.AssignmentResponse;
import dev.as_request.dto.TechnicianResponse;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

@Service
public class TechnicianService {

	private final RestClient technicianRestClient;

	public TechnicianService(@Qualifier("technicianRestClient") RestClient technicianRestClient) {
		this.technicianRestClient = technicianRestClient;
	}

	public List<TechnicianResponse> getAvailableTechnicians(String region, String productType, LocalDate date, String timeSlot) {

		return technicianRestClient.get()
			.uri(builder -> builder.path("/api/assignments/technicians/available")
				.queryParam("region", region)
				.queryParam("productType", productType)
				.queryParam("date", date.toString())
				.queryParam("timeSlot", timeSlot)
				.build())
			.retrieve()
			.body(new ParameterizedTypeReference<List<TechnicianResponse>>() {});
	}

	public AssignmentResponse assignTechnician(AssignmentRequest request) {
		return technicianRestClient.post()
			.uri("/api/assignments")
			.contentType(MediaType.APPLICATION_JSON)
			.body(request)
			.retrieve()
			.body(AssignmentResponse.class);
	}

}