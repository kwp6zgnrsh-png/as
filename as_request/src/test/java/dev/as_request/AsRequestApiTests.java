package dev.as_request;

import dev.as_request.dto.ProductResponse;
import dev.as_request.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Sql("/db/repairs-test.sql")
class AsRequestApiTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void returnsProductsFromProductService() throws Exception {
        when(productService.getProducts()).thenReturn(List.of(
                new ProductResponse(1L, "공기청정기", "AIR_PURIFIER")
        ));
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].productId").value(1))
                .andExpect(jsonPath("$[0].productName").value("공기청정기"))
                .andExpect(jsonPath("$[0].productType").value("AIR_PURIFIER"));
    }

    @Test
    void returnsBadGatewayWhenProductServiceFails() throws Exception {
        when(productService.getProducts()).thenThrow(new RestClientException("upstream failure"));
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("제품 서버에서 정보를 가져오지 못했습니다. 잠시 후 다시 시도해 주세요."));
    }

    @Test
    void createsTemporaryRepairResponseAfterRequestingProductDetail() throws Exception {
        when(productService.getProduct(101L)).thenReturn(new ProductResponse(101L, "공기청정기", "AIR_PURIFIER"));
        String visitDateTime = LocalDateTime.now().plusDays(1).withNano(0).toString();
        mockMvc.perform(post("/api/repairs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(visitDateTime)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestId").isNumber())
                .andExpect(jsonPath("$.productId").value(101))
                .andExpect(jsonPath("$.customerName").value("테스트"))
                .andExpect(jsonPath("$.status").value("RECEIVED"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.symptom").value("전원이 켜지지 않습니다."));
        verify(productService).getProduct(101L);
    }

    @Test
    void stopsRepairRequestWhenProductLookupFails() throws Exception {
        when(productService.getProduct(101L)).thenThrow(new RestClientException("upstream failure"));
        mockMvc.perform(post("/api/repairs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(LocalDateTime.now().plusDays(1).toString())))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("제품 서버에서 정보를 가져오지 못했습니다. 잠시 후 다시 시도해 주세요."));
        verify(productService).getProduct(101L);
    }

    @Test
    void readsDatabaseRowsInNewestOrderWithNullableFields() throws Exception {
        mockMvc.perform(get("/api/repairs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0]", org.hamcrest.Matchers.aMapWithSize(10)))
                .andExpect(jsonPath("$[0].requestId").value(3))
                .andExpect(jsonPath("$[0].requestNo").value("AS-TEST-0003"))
                .andExpect(jsonPath("$[0].customerName").value("최근접수"))
                .andExpect(jsonPath("$[0].phone").value("01000000003"))
                .andExpect(jsonPath("$[0].productId").value(103))
                .andExpect(jsonPath("$[0].region").value("경기도"))
                .andExpect(jsonPath("$[0].symptom").value(nullValue()))
                .andExpect(jsonPath("$[0].technicianId").value(nullValue()))
                .andExpect(jsonPath("$[0].status").value("WAITING_ASSIGNMENT"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-10-20T10:00:00"))
                .andExpect(jsonPath("$[1].requestId").value(2))
                .andExpect(jsonPath("$[1].technicianId").value(701))
                .andExpect(jsonPath("$[1].symptom").value("전원이 켜지지 않습니다."))
                .andExpect(jsonPath("$[2].requestId").value(1));
    }

    @Test
    @Sql(scripts = "/db/repairs-test.sql", statements = "DELETE FROM as_request")
    void returnsEmptyListWhenDatabaseHasNoRepairs() throws Exception {
        mockMvc.perform(get("/api/repairs"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
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
        verifyNoInteractions(productService);
    }

    @Test
    void allowsLocalFrontendCorsPreflight() throws Exception {
        mockMvc.perform(options("/api/repairs")
                        .header("Origin", "http://localhost:5174")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5174"));
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
