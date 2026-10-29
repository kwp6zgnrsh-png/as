package dev.as_request.service;

import dev.as_request.dto.AssignmentRequest;
import dev.as_request.dto.AssignmentResponse;
import dev.as_request.dto.TechnicianResponse;
import dev.as_request.exception.AssignmentFailedException;
import dev.as_request.exception.TechnicianServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class TechnicianServiceTest {

    private static final String BASE_URL = "http://technician-service.test";
    private static final LocalDate DATE = LocalDate.of(2099, 1, 1);
    private static final URI AVAILABLE_URI = URI.create(BASE_URL
            + "/api/assignments/technicians/available?region=SEOUL&productType=AIR_PURIFIER"
            + "&date=2099-01-01&timeSlot=MORNING");
    private static final URI ASSIGNMENT_URI = URI.create(BASE_URL + "/api/assignments");
    private static final AssignmentRequest ASSIGNMENT =
            new AssignmentRequest(1L, 701L, DATE.atTime(10, 0), "MORNING");

    @Mock
    private ClientHttpRequestFactory requestFactory;

    private TechnicianService technicianService;

    @BeforeEach
    void setUp() {
        technicianService = new TechnicianService(RestClient.builder()
                .baseUrl(BASE_URL).requestFactory(requestFactory).build());
    }

    @Test
    @DisplayName("지역, 제품 종류, 날짜, 시간대로 배정 가능한 기사를 조회한다")
    void getsAvailableTechnicians() throws IOException {
        // Given
        given(requestFactory.createRequest(AVAILABLE_URI, HttpMethod.GET)).willReturn(responseRequest("""
                [{"technicianId":701,"technicianName":"김기사","region":"SEOUL","active":true}]
                """, HttpStatus.OK));

        // When
        List<TechnicianResponse> result = technicianService.getAvailableTechnicians(
                "SEOUL", "AIR_PURIFIER", DATE, "MORNING");

        // Then
        assertThat(result).containsExactly(new TechnicianResponse(701L, "김기사", "SEOUL", true));
        then(requestFactory).should().createRequest(AVAILABLE_URI, HttpMethod.GET);
    }

    @Test
    @DisplayName("배정 가능한 기사가 없으면 빈 목록을 반환한다")
    void returnsEmptyTechnicians() throws IOException {
        // Given
        given(requestFactory.createRequest(AVAILABLE_URI, HttpMethod.GET))
                .willReturn(responseRequest("[]", HttpStatus.OK));

        // When
        List<TechnicianResponse> result = technicianService.getAvailableTechnicians(
                "SEOUL", "AIR_PURIFIER", DATE, "MORNING");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("기사 배정 요청을 JSON으로 전송하고 배정 결과를 반환한다")
    void assignsTechnician() throws IOException {
        // Given
        MockClientHttpRequest httpRequest = responseRequest("""
                {"assignmentId":10,"requestId":1,"technicianId":701,
                 "status":"ASSIGNED","assignedAt":"2099-01-01T09:00:00"}
                """, HttpStatus.CREATED);
        given(requestFactory.createRequest(ASSIGNMENT_URI, HttpMethod.POST)).willReturn(httpRequest);

        // When
        AssignmentResponse result = technicianService.assignTechnician(ASSIGNMENT);

        // Then
        assertThat(result).isEqualTo(new AssignmentResponse(
                10L, 1L, 701L, "ASSIGNED", LocalDateTime.of(2099, 1, 1, 9, 0)));
        assertThat(httpRequest.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        org.springframework.test.json.JsonAssert.comparator(org.springframework.test.json.JsonCompareMode.STRICT)
                .assertIsMatch("""
                        {"requestId":1,"technicianId":701,"date":"2099-01-01T10:00:00","timeSlot":"MORNING"}
                        """, httpRequest.getBodyAsString());
        then(requestFactory).should().createRequest(ASSIGNMENT_URI, HttpMethod.POST);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("기사 조회 또는 배정 응답 본문이 없으면 해당 서비스 예외를 발생시킨다")
    void rejectsEmptyResponse(boolean assignment) throws IOException {
        // Given
        given(requestFactory.createRequest(assignment ? ASSIGNMENT_URI : AVAILABLE_URI,
                assignment ? HttpMethod.POST : HttpMethod.GET)).willReturn(responseRequest("", HttpStatus.OK));

        // When
        Throwable thrown = catchThrowable(() -> {
            if (assignment) technicianService.assignTechnician(ASSIGNMENT);
            else technicianService.getAvailableTechnicians("SEOUL", "AIR_PURIFIER", DATE, "MORNING");
        });

        // Then
        assertThat(thrown).isInstanceOf(assignment ? AssignmentFailedException.class : TechnicianServiceException.class)
                .hasMessageContaining("응답 본문이 비어 있습니다");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("기사 서버 HTTP 오류를 조회 또는 배정 예외로 변환한다")
    void wrapsServerError(boolean assignment) throws IOException {
        // Given
        given(requestFactory.createRequest(assignment ? ASSIGNMENT_URI : AVAILABLE_URI,
                assignment ? HttpMethod.POST : HttpMethod.GET))
                .willReturn(responseRequest("{}", HttpStatus.INTERNAL_SERVER_ERROR));

        // When
        Throwable thrown = catchThrowable(() -> {
            if (assignment) technicianService.assignTechnician(ASSIGNMENT);
            else technicianService.getAvailableTechnicians("SEOUL", "AIR_PURIFIER", DATE, "MORNING");
        });

        // Then
        assertThat(thrown).isInstanceOf(assignment ? AssignmentFailedException.class : TechnicianServiceException.class)
                .hasCauseInstanceOf(RestClientException.class);
    }

    private MockClientHttpRequest responseRequest(String body, HttpStatus status) {
        MockClientHttpResponse response = new MockClientHttpResponse(body.getBytes(StandardCharsets.UTF_8), status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        MockClientHttpRequest request = new MockClientHttpRequest();
        request.setResponse(response);
        return request;
    }
}
