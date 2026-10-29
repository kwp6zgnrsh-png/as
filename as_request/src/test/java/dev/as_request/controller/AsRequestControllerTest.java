package dev.as_request.controller;

import dev.as_request.dto.CreateRepairRequest;
import dev.as_request.dto.ProductResponse;
import dev.as_request.dto.RepairHistoryRequest;
import dev.as_request.dto.RepairListResponse;
import dev.as_request.exception.AssignmentFailedException;
import dev.as_request.exception.ProductServiceException;
import dev.as_request.exception.TechnicianNotFoundException;
import dev.as_request.exception.TechnicianServiceException;
import dev.as_request.service.ProductService;
import dev.as_request.service.RepairService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AsRequestController.class)
class AsRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ProductService productService;
    @MockitoBean
    private RepairService repairService;

    @Test
    @DisplayName("제품 목록 조회는 200과 제품 배열을 반환한다")
    void getsProducts() throws Exception {
        // Given
        given(productService.getProducts()).willReturn(List.of(
                new ProductResponse(101L, "공기청정기", "AIR_PURIFIER")));

        // When
        ResultActions result = mockMvc.perform(get("/api/products"));

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].productId").value(101))
                .andExpect(jsonPath("$[0].productName").value("공기청정기"))
                .andExpect(jsonPath("$[0].productType").value("AIR_PURIFIER"));
        then(productService).should().getProducts();
    }

    @Test
    @DisplayName("정상 접수는 201과 접수 내역 조회 HAL 링크를 반환한다")
    void createsRepair() throws Exception {
        // Given
        LocalDateTime visit = tomorrowAt(10);
        CreateRepairRequest request = new CreateRepairRequest(
                101L, "홍길동", "01012345678", "SEOUL", visit, "전원 불량");
        willDoNothing().given(repairService).createRepair(request);

        // When
        ResultActions result = mockMvc.perform(post("/api/repairs")
                .contentType(MediaType.APPLICATION_JSON).content(repairJson(visit)));

        // Then
        result.andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaTypes.HAL_JSON))
                .andExpect(jsonPath("$._links['search-repairs'].href")
                        .value("http://localhost/api/repairs/search"));
        then(repairService).should().createRepair(request);
    }

    @Test
    @DisplayName("이름과 전화번호로 본인의 접수 내역을 조회한다")
    void searchesMyRepairs() throws Exception {
        // Given
        RepairHistoryRequest request = new RepairHistoryRequest("홍길동", "01012345678");
        given(repairService.getMyRepairs(request)).willReturn(List.of(repairHistory()));

        // When
        ResultActions result = mockMvc.perform(post("/api/repairs/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"customerName":"홍길동","phone":"01012345678"}
                        """));

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].requestNo").value("AS-1"))
                .andExpect(jsonPath("$[0].customerName").value("홍길동"))
                .andExpect(jsonPath("$[0].phone").value("01012345678"));
        then(repairService).should().getMyRepairs(request);
    }

    @Test
    @DisplayName("전체 접수 목록은 200과 접수 배열을 반환한다")
    void getsRepairs() throws Exception {
        // Given
        given(repairService.getRepairs()).willReturn(List.of(repairHistory()));

        // When
        ResultActions result = mockMvc.perform(get("/api/repairs"));

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].requestNo").value("AS-1"))
                .andExpect(jsonPath("$[0].region").value("SEOUL"))
                .andExpect(jsonPath("$[0].symptom").value("전원 불량"))
                .andExpect(jsonPath("$[0].status").value("ASSIGNED"))
                .andExpect(jsonPath("$[0].createdAt").value("2099-01-01T10:00:00"));
        then(repairService).should().getRepairs();
    }

    @Test
    @DisplayName("접수 내역이 없으면 빈 JSON 배열을 반환한다")
    void getsEmptyRepairs() throws Exception {
        // Given
        given(repairService.getRepairs()).willReturn(List.of());

        // When
        ResultActions result = mockMvc.perform(get("/api/repairs"));

        // Then
        result.andExpect(status().isOk()).andExpect(content().json("[]"));
        then(repairService).should().getRepairs();
    }

    @ParameterizedTest
    @ValueSource(strings = {"010-1234-5678", "01112345678", "0101234", ""})
    @DisplayName("연락처 형식이 잘못되면 400을 반환하고 접수하지 않는다")
    void rejectsInvalidPhone(String phone) throws Exception {
        // Given
        String body = repairJson(tomorrowAt(10)).replace("01012345678", phone);

        // When
        ResultActions result = mockMvc.perform(post("/api/repairs")
                .contentType(MediaType.APPLICATION_JSON).content(body));

        // Then
        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
        then(repairService).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(strings = {"seoul", "서울특별시", "DAEGU"})
    @DisplayName("지원하지 않는 지역이면 400을 반환한다")
    void rejectsInvalidRegion(String region) throws Exception {
        // Given
        String body = repairJson(tomorrowAt(10)).replace("SEOUL", region);

        // When
        ResultActions result = mockMvc.perform(post("/api/repairs")
                .contentType(MediaType.APPLICATION_JSON).content(body));

        // Then
        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
        then(repairService).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(strings = {"08:59", "12:00", "12:59", "18:00"})
    @DisplayName("방문 가능 시간 밖의 신청은 400을 반환한다")
    void rejectsInvalidVisitTime(String time) throws Exception {
        // Given
        LocalDateTime visit = LocalDate.now().plusDays(1).atTime(LocalTime.parse(time));

        // When
        ResultActions result = mockMvc.perform(post("/api/repairs")
                .contentType(MediaType.APPLICATION_JSON).content(repairJson(visit)));

        // Then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("방문 시간은 오전 09:00~12:00 또는 오후 13:00~18:00이어야 합니다."));
        then(repairService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("과거 방문 일시의 신청은 400을 반환한다")
    void rejectsPastVisit() throws Exception {
        // Given
        LocalDateTime visit = LocalDate.now().minusDays(1).atTime(10, 0);

        // When
        ResultActions result = mockMvc.perform(post("/api/repairs")
                .contentType(MediaType.APPLICATION_JSON).content(repairJson(visit)));

        // Then
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("희망 방문 일시는 현재 이후여야 합니다."));
        then(repairService).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{", "{\"productId\":\"invalid\"}"})
    @DisplayName("필수값이 없거나 JSON 형식이 잘못되면 400을 반환한다")
    void rejectsInvalidBody(String body) throws Exception {
        // Given
        var request = post("/api/repairs").contentType(MediaType.APPLICATION_JSON).content(body);

        // When
        ResultActions result = mockMvc.perform(request);

        // Then
        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
        then(repairService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("내역 조회의 입력값이 잘못되면 400을 반환한다")
    void rejectsInvalidHistoryRequest() throws Exception {
        // Given
        String body = """
                {"customerName":"홍길동","phone":"010-1234-5678"}
                """;

        // When
        ResultActions result = mockMvc.perform(post("/api/repairs/search")
                .contentType(MediaType.APPLICATION_JSON).content(body));

        // Then
        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
        then(repairService).shouldHaveNoInteractions();
    }

    @ParameterizedTest
    @MethodSource("repairFailures")
    @DisplayName("접수 오류를 해당 HTTP 상태와 안내 메시지로 변환한다")
    void mapsRepairFailure(RuntimeException failure, int expectedStatus, String expectedMessage) throws Exception {
        // Given
        willThrow(failure).given(repairService).createRepair(any(CreateRepairRequest.class));

        // When
        ResultActions result = mockMvc.perform(post("/api/repairs")
                .contentType(MediaType.APPLICATION_JSON).content(repairJson(tomorrowAt(10))));

        // Then
        result.andExpect(status().is(expectedStatus)).andExpect(jsonPath("$.message").value(expectedMessage));
        then(repairService).should().createRepair(any(CreateRepairRequest.class));
    }

    private static Stream<Arguments> repairFailures() {
        return Stream.of(
                Arguments.of(new TechnicianNotFoundException(), 409, "선택한 지역과 시간대에 배정 가능한 기사가 없습니다."),
                Arguments.of(new ProductServiceException("제품 조회 실패"), 502, "제품 조회 실패"),
                Arguments.of(new TechnicianServiceException("기사 조회 실패"), 502, "기사 조회 실패"),
                Arguments.of(new AssignmentFailedException("기사 배정 실패"), 502, "기사 배정 실패"),
                Arguments.of(new DataAccessResourceFailureException("DB 연결 실패"), 503,
                        "접수 목록을 조회할 수 없습니다. DB 연결 및 설정을 확인해 주세요.")
        );
    }

    private LocalDateTime tomorrowAt(int hour) {
        return LocalDate.now().plusDays(1).atTime(hour, 0);
    }

    private String repairJson(LocalDateTime visit) {
        return """
                {"productId":101,"customerName":"홍길동","phone":"01012345678", "region":"SEOUL",
                 "visitDateTime":"%s","memo":"전원 불량"}
                """.formatted(visit);
    }

    private RepairListResponse repairHistory() {
        return new RepairListResponse("AS-1", "홍길동", "01012345678", "SEOUL",
                "전원 불량", "ASSIGNED", LocalDateTime.of(2099, 1, 1, 10, 0));
    }
}
