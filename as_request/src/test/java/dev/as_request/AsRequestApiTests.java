package dev.as_request;

import dev.as_request.dto.ProductResponse;
import dev.as_request.dto.AssignmentRequest;
import dev.as_request.dto.AssignmentResponse;
import dev.as_request.dto.TechnicianResponse;
import dev.as_request.service.ProductService;
import dev.as_request.service.TechnicianService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import dev.as_request.exception.ProductServiceException;
import dev.as_request.exception.TechnicianServiceException;
import dev.as_request.exception.AssignmentFailedException;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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

    @MockitoBean
    private TechnicianService technicianService;

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
        when(productService.getProducts()).thenThrow(new ProductServiceException("제품 서버에서 정보를 가져오지 못했습니다. 잠시 후 다시 시도해 주세요."));
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("제품 서버에서 정보를 가져오지 못했습니다. 잠시 후 다시 시도해 주세요."));
    }

    @Test
    void createsRepairAndRequestsTechnicianAssignment() throws Exception {
        LocalDateTime visitDateTime = futureVisit();
        when(productService.getProduct(101L)).thenReturn(new ProductResponse(101L, "공기청정기", "AIR_PURIFIER"));
        when(technicianService.getAvailableTechnicians("SEOUL", "AIR_PURIFIER", visitDateTime.toLocalDate(), "MORNING"))
                .thenReturn(List.of(new TechnicianResponse(701L, "테스트기사", "SEOUL", true)));
        when(technicianService.assignTechnician(any())).thenAnswer(invocation -> {
            AssignmentRequest assignment = invocation.getArgument(0);
            return new AssignmentResponse(1L, assignment.requestId(), assignment.technicianId(), "ASSIGNED", assignment.date());
        });

        mockMvc.perform(post("/api/repairs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(visitDateTime.toString())))
                .andExpect(status().isCreated())
                .andExpect(content().string(""));

        verify(productService).getProduct(101L);
        verify(technicianService).getAvailableTechnicians("SEOUL", "AIR_PURIFIER", visitDateTime.toLocalDate(), "MORNING");
        ArgumentCaptor<AssignmentRequest> assignment = ArgumentCaptor.forClass(AssignmentRequest.class);
        verify(technicianService).assignTechnician(assignment.capture());
        assertEquals(4L, assignment.getValue().requestId());
        assertEquals(701L, assignment.getValue().technicianId());
        assertEquals(visitDateTime, assignment.getValue().date());
        assertEquals("MORNING", assignment.getValue().timeSlot());

        mockMvc.perform(get("/api/repairs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].requestNo").value("AS-4"))
                .andExpect(jsonPath("$[0].status").value("ASSIGNED"));
    }

    @Test
    void stopsRepairRequestWhenProductLookupFails() throws Exception {
        when(productService.getProduct(101L)).thenThrow(new ProductServiceException("제품 서버에서 정보를 가져오지 못했습니다. 잠시 후 다시 시도해 주세요."));
        mockMvc.perform(post("/api/repairs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(futureVisit().toString())))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("제품 서버에서 정보를 가져오지 못했습니다. 잠시 후 다시 시도해 주세요."));
        verify(productService).getProduct(101L);
    }

    @Test
    void readsDatabaseRowsInNewestOrderWithNullableFields() throws Exception {
        mockMvc.perform(get("/api/repairs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0]", org.hamcrest.Matchers.aMapWithSize(7)))
                .andExpect(jsonPath("$[0].requestNo").value("AS-TEST-0003"))
                .andExpect(jsonPath("$[0].customerName").value("최근접수"))
                .andExpect(jsonPath("$[0].phone").value("01000000003"))
                .andExpect(jsonPath("$[0].region").value("경기도"))
                .andExpect(jsonPath("$[0].symptom").value(nullValue()))
                .andExpect(jsonPath("$[0].status").value("WAITING_ASSIGNMENT"))
                .andExpect(jsonPath("$[0].createdAt").value("2026-10-20T10:00:00"))
                .andExpect(jsonPath("$[1].symptom").value("전원이 켜지지 않습니다."))
                .andExpect(jsonPath("$[2].requestNo").value("AS-TEST-0001"));
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
                        .content(validRequest(LocalDateTime.now().minusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0).toString())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("희망 방문 일시는 현재 이후여야 합니다."));
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
        verifyNoInteractions(productService, technicianService);
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

    @Test
    void returnsConflictWhenNoTechniciansAreAvailable() throws Exception {
        LocalDateTime visit = futureVisit();
        when(productService.getProduct(101L)).thenReturn(new ProductResponse(101L, "공기청정기", "AIR_PURIFIER"));
        when(technicianService.getAvailableTechnicians("SEOUL", "AIR_PURIFIER", visit.toLocalDate(), "MORNING"))
                .thenReturn(List.of());

        mockMvc.perform(post("/api/repairs").contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(visit.toString())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("선택한 지역과 시간대에 배정 가능한 기사가 없습니다."));
        mockMvc.perform(get("/api/repairs"))
                .andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void identifiesTechnicianServerFailure() throws Exception {
        LocalDateTime visit = futureVisit();
        when(productService.getProduct(101L)).thenReturn(new ProductResponse(101L, "공기청정기", "AIR_PURIFIER"));
        when(technicianService.getAvailableTechnicians("SEOUL", "AIR_PURIFIER", visit.toLocalDate(), "MORNING"))
                .thenThrow(new TechnicianServiceException("기사 서버 조회 실패"));

        mockMvc.perform(post("/api/repairs").contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(visit.toString())))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("기사 서버 조회 실패"));
    }

    @Test
    void leavesRequestReceivedWhenAssignmentHasNoBody() throws Exception {
        LocalDateTime visit = futureVisit();
        when(productService.getProduct(101L)).thenReturn(new ProductResponse(101L, "공기청정기", "AIR_PURIFIER"));
        when(technicianService.getAvailableTechnicians("SEOUL", "AIR_PURIFIER", visit.toLocalDate(), "MORNING"))
                .thenReturn(List.of(new TechnicianResponse(701L, "테스트기사", "SEOUL", true)));
        when(technicianService.assignTechnician(any())).thenReturn(null);

        mockMvc.perform(post("/api/repairs").contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(visit.toString())))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("기사 배정 응답 본문이 비어 있습니다."));
        mockMvc.perform(get("/api/repairs"))
                .andExpect(jsonPath("$[0].status").value("RECEIVED"));
    }

    @Test
    void handlesAssignmentServerFailure() throws Exception {
        LocalDateTime visit = futureVisit();
        when(productService.getProduct(101L)).thenReturn(new ProductResponse(101L, "공기청정기", "AIR_PURIFIER"));
        when(technicianService.getAvailableTechnicians("SEOUL", "AIR_PURIFIER", visit.toLocalDate(), "MORNING"))
                .thenReturn(List.of(new TechnicianResponse(701L, "테스트기사", "SEOUL", true)));
        when(technicianService.assignTechnician(any()))
                .thenThrow(new AssignmentFailedException("기사 서버에 배정을 요청하지 못했습니다."));

        mockMvc.perform(post("/api/repairs").contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest(visit.toString())))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("기사 서버에 배정을 요청하지 못했습니다."));
        mockMvc.perform(get("/api/repairs"))
                .andExpect(jsonPath("$[0].status").value("RECEIVED"));
    }

    private LocalDateTime futureVisit() {
        return LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
    }

    private String validRequest(String visitDateTime) {
        return """
                {
                  "productId": 101,
                  "customerName": "테스트",
                  "phone": "01000000000",
                  "region": "SEOUL",
                  "visitDateTime": "%s",
                  "memo": "전원이 켜지지 않습니다."
                }
                """.formatted(visitDateTime);
    }
}
