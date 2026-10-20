package dev.as_request;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/db/repairs-test.sql")
class RepairHistoryApiTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void addHistoryRows() {
        jdbcTemplate.update("""
                INSERT INTO as_request
                    (request_id, request_no, customer_name, phone, product_id,
                     region, symptom, technician_id, status, created_at)
                VALUES
                    (10, 'AS-10', '홍길동', '01012345678', 1, 'SEOUL', '첫 문의', NULL, 'RECEIVED', '2026-10-19 10:00:00'),
                    (11, 'AS-11', '홍길동', '01012345678', 1, 'SEOUL', '두 번째', 701, 'ASSIGNED', '2026-10-20 10:00:00'),
                    (12, 'AS-12', '홍길동', '01012345678', 1, 'SEOUL', NULL, NULL, 'RECEIVED', '2026-10-20 10:00:00'),
                    (13, 'AS-13', '홍길동', '01087654321', 1, 'SEOUL', NULL, NULL, 'RECEIVED', '2026-10-21 10:00:00'),
                    (14, 'AS-14', '김철수', '01012345678', 1, 'SEOUL', NULL, NULL, 'RECEIVED', '2026-10-21 10:00:00')
                """);
    }

    @Test
    void returnsOnlyMatchingNameAndPhoneInNewestOrder() throws Exception {
        mockMvc.perform(get("/api/repairs/search")
                        .param("customerName", "홍길동")
                        .param("phone", "01012345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].requestNo", contains("AS-12", "AS-11", "AS-10")))
                .andExpect(jsonPath("$[*].customerName", everyItem(is("홍길동"))))
                .andExpect(jsonPath("$[*].phone", everyItem(is("01012345678"))))
                .andExpect(jsonPath("$[0]", aMapWithSize(7)))
                .andExpect(jsonPath("$[0].symptom").value(nullValue()))
                .andExpect(jsonPath("$[1].status").value("ASSIGNED"));
    }

    @Test
    void returnsEmptyListWhenIdentityDoesNotMatch() throws Exception {
        mockMvc.perform(get("/api/repairs/search")
                        .param("customerName", "없는고객")
                        .param("phone", "01012345678"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void treatsSqlSyntaxInNameAsLiteralInput() throws Exception {
        mockMvc.perform(get("/api/repairs/search")
                        .param("customerName", "가' OR'1")
                        .param("phone", "01012345678"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @ParameterizedTest
    @CsvSource({
            "김,01012345678",
            "가나다라마바사아자,01012345678",
            "홍길동,010-1234-5678",
            "홍길동,01112345678",
            "홍길동,0101234567",
            "홍길동,010123456789"
    })
    void rejectsInvalidIdentity(String customerName, String phone) throws Exception {
        mockMvc.perform(get("/api/repairs/search")
                        .param("customerName", customerName)
                        .param("phone", phone))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void requiresBothNameAndPhone() throws Exception {
        mockMvc.perform(get("/api/repairs/search").param("customerName", "홍길동"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/repairs/search").param("phone", "01012345678"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/repairs/search").param("customerName", " ").param("phone", ""))
                .andExpect(status().isBadRequest());
    }
}
