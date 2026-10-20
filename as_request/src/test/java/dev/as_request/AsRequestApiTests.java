package dev.as_request;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AsRequestApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void productsReturnEmptyListUntilProductServerIsConnected() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void createsRepairAndIncludesItInList() throws Exception {
        String visitDateTime = LocalDateTime.now().plusDays(1).withNano(0).toString();
        mockMvc.perform(post("/api/repairs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(visitDateTime)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestId").isNumber())
                .andExpect(jsonPath("$.productId").value(101))
                .andExpect(jsonPath("$.customerName").value("테스트"))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.createdAt").exists());

        mockMvc.perform(get("/api/repairs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productId").value(101))
                .andExpect(jsonPath("$[0].phone").value("01000000000"))
                .andExpect(jsonPath("$[0].region").value("서울특별시"))
                .andExpect(jsonPath("$[0].memo").value("전원이 켜지지 않습니다."));
    }

    @Test
    void rejectsPastVisitDateTime() throws Exception {
        mockMvc.perform(post("/api/repairs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(LocalDateTime.now().minusDays(1).toString())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("희망 방문 일시는 현재 시간 이후로 선택해 주세요."));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"productId\":0}",
            "{\"productId\":101,\"customerName\":\"   \"}",
            "{\"productId\":101,\"customerName\":\"테스트\",\"phone\":\"010-0000-0000\"}",
            "{\"productId\":101,\"customerName\":\"테스트\",\"phone\":\"01000000000\",\"region\":\" \"}",
            "{\"productId\":101,\"customerName\":\"테스트\",\"phone\":\"01000000000\",\"region\":\"서울특별시\"}",
            "{\"visitDateTime\":\"invalid-date\"}",
            "not-json"
    })
    void rejectsInvalidRequests(String body) throws Exception {
        mockMvc.perform(post("/api/repairs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void allowsLocalFrontendCorsPreflight() throws Exception {
        mockMvc.perform(options("/api/repairs")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    private String validRequest(String visitDateTime) {
        return """
                {
                  "productId": 101,
                  "customerName": "테스트",
                  "phone": "01000000000",
                  "region": "서울특별시",
                  "visitDateTime": "%s",
                  "memo": "전원이 켜지지 않습니다."
                }
                """.formatted(visitDateTime);
    }
}
