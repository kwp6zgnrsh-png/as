package dev.as_request;

import dev.as_request.controller.AsRequestController;
import dev.as_request.exception.GlobalExceptionHandler;
import dev.as_request.service.ProductService;
import dev.as_request.service.RepairService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CreateRepairRequestValidationTests {
    private MockMvc mockMvc;
    private RepairService repairService;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        repairService = mock(RepairService.class);
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AsRequestController(mock(ProductService.class), repairService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        validator.close();
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void rejectsInvalidFieldsBeforeCallingService(String body, String message) throws Exception {
        mockMvc.perform(post("/api/repairs").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(message));
        verifyNoInteractions(repairService);
    }

    static Stream<Arguments> invalidRequests() {
        String nameMessage = "신청자명은 2자 이상 8자 이하로 입력해 주세요.";
        String phoneMessage = "연락처는 하이픈 없이 010으로 시작하는 11자리 숫자로 입력해 주세요.";
        String regionMessage = "지역은 seoul 또는 busan만 입력할 수 있습니다.";
        return Stream.of(
                Arguments.of(body("null", "테스트", "01044443333", "SEOUL", ""), "제품 ID를 입력해 주세요."),
                Arguments.of(body("0", "테스트", "01044443333", "SEOUL", ""), "제품 ID는 양수여야 합니다."),
                Arguments.of(body("-1", "테스트", "01044443333", "SEOUL", ""), "제품 ID는 양수여야 합니다."),
                Arguments.of(body("1", "김", "01044443333", "SEOUL", ""), nameMessage),
                Arguments.of(body("1", "가나다라마바사아자", "01044443333", "SEOUL", ""), nameMessage),
                Arguments.of(body("1", "테스트", "010-4444-3333", "SEOUL", ""), phoneMessage),
                Arguments.of(body("1", "테스트", "0104444333", "SEOUL", ""), phoneMessage),
                Arguments.of(body("1", "테스트", "010444433333", "SEOUL", ""), phoneMessage),
                Arguments.of(body("1", "테스트", "01144443333", "SEOUL", ""), phoneMessage),
                Arguments.of(body("1", "테스트", "0104444aaaa", "SEOUL", ""), phoneMessage),
                Arguments.of(body("1", "테스트", "01044443333", "seoul", ""), regionMessage),
                Arguments.of(body("1", "테스트", "01044443333", "daegu", ""), regionMessage),
                Arguments.of(body("1", "테스트", "01044443333", "SEOUL", "가".repeat(1001)), "메모는 1000자 이하로 입력해 주세요.")
        );
    }

    @ParameterizedTest
    @MethodSource("validRequests")
    void acceptsBoundaryValues(String body) throws Exception {
        mockMvc.perform(post("/api/repairs").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().is2xxSuccessful());
        verify(repairService).createRepair(any());
    }

    static Stream<Arguments> validRequests() {
        return Stream.of(
                Arguments.of(body("1", "김철", "01044443333", "SEOUL", "가".repeat(1000))),
                Arguments.of(body("1", "가나다라마바사아", "01044443333", "BUSAN", "")),
                Arguments.of(body("1", "테스트", "01044443333", "SEOUL", null))
        );
    }

    @ParameterizedTest
    @MethodSource("invalidVisitDateTimes")
    void rejectsInvalidVisitDateTimeBeforeCallingService(String date, String message) throws Exception {
        mockMvc.perform(post("/api/repairs").contentType(MediaType.APPLICATION_JSON)
                        .content(body("1", "테스트", "01044443333", "SEOUL", "", date)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(message));
        verifyNoInteractions(repairService);
    }

    static Stream<Arguments> invalidVisitDateTimes() {
        String timeMessage = "방문 시간은 오전 09:00~12:00 또는 오후 13:00~18:00이어야 합니다.";
        return Stream.of(
                Arguments.of(null, "희망 방문 일시를 입력해 주세요."),
                Arguments.of(LocalDateTime.now().minusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0).toString(),
                        "희망 방문 일시는 현재 이후여야 합니다."),
                Arguments.of(futureVisit(8, 59).toString(), timeMessage),
                Arguments.of(futureVisit(12, 0).toString(), timeMessage),
                Arguments.of(futureVisit(12, 30).toString(), timeMessage),
                Arguments.of(futureVisit(18, 0).toString(), timeMessage),
                Arguments.of(futureVisit(18, 1).toString(), timeMessage)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"09:00", "09:06", "11:59", "11:59:59", "13:00", "14:00", "17:59:59"})
    void acceptsVisitTimeBoundaries(String time) throws Exception {
        String date = LocalDateTime.now().toLocalDate().plusDays(1) + "T" + time;
        mockMvc.perform(post("/api/repairs").contentType(MediaType.APPLICATION_JSON)
                        .content(body("1", "테스트", "01044443333", "SEOUL", "", date)))
                .andExpect(status().is2xxSuccessful());
        verify(repairService).createRepair(any());
    }

    @Test
    void rejectsNonNumericProductId() throws Exception {
        mockMvc.perform(post("/api/repairs").contentType(MediaType.APPLICATION_JSON)
                        .content(body("\"abc\"", "테스트", "01044443333", "SEOUL", "")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(repairService);
    }

    private static LocalDateTime futureVisit(int hour, int minute) {
        return LocalDateTime.now().plusDays(1).withHour(hour).withMinute(minute).withSecond(0).withNano(0);
    }

    private static String body(String productId, String name, String phone, String region, String memo) {
        return body(productId, name, phone, region, memo, futureVisit(14, 0).toString());
    }

    private static String body(String productId, String name, String phone, String region, String memo, String date) {
        String memoValue = memo == null ? "null" : '"' + memo + '"';
        String dateValue = date == null ? "null" : '"' + date + '"';
        return """
                {"productId":%s,"customerName":"%s","phone":"%s","region":"%s",
                 "visitDateTime":%s,"memo":%s}
                """.formatted(productId, name, phone, region, dateValue, memoValue);
    }
}
