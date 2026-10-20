package dev.as_request.service;

import dev.as_request.dto.AssignmentRequest;
import dev.as_request.dto.AssignmentResponse;
import dev.as_request.dto.TechnicianResponse;
import dev.as_request.exception.AssignmentFailedException;
import dev.as_request.exception.TechnicianServiceException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.List;

@Service
public class TechnicianService {

    private final RestClient technicianRestClient;

    public TechnicianService(@Qualifier("technicianRestClient") RestClient technicianRestClient) {
        this.technicianRestClient = technicianRestClient;
    }

    public List<TechnicianResponse> getAvailableTechnicians(String region,
                                                            String productType,
                                                            LocalDate date,
                                                            String timeSlot) {

        List<TechnicianResponse> technicians;

        try {
            technicians = technicianRestClient.get()
                    .uri(builder -> builder.path("/api/assignments/technicians/available")
                            .queryParam("region", region)
                            .queryParam("productType", productType)
                            .queryParam("date", date.toString())
                            .queryParam("timeSlot", timeSlot)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<TechnicianResponse>>() {});
        } catch (RestClientException exception) {
            throw new TechnicianServiceException("기사 서버에서 배정 가능한 기사를 조회하지 못했습니다. 잠시 후 다시 시도해 주세요.", exception);
        }
        if (technicians == null) {
            throw new TechnicianServiceException("기사 서버의 조회 응답 본문이 비어 있습니다.");
        }
        return technicians;
    }

    public AssignmentResponse assignTechnician(AssignmentRequest request) {
        AssignmentResponse assignment;
        try {
            assignment = technicianRestClient.post()
                    .uri("/api/assignments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AssignmentResponse.class);
        } catch (RestClientException exception) {
            throw new AssignmentFailedException("기사 서버에 배정을 요청하지 못했습니다.", exception);
        }
        if (assignment == null) {
            throw new AssignmentFailedException("기사 배정 응답 본문이 비어 있습니다.");
        }
        return assignment;
    }
}
