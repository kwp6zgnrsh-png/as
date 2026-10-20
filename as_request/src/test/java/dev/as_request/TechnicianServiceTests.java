package dev.as_request;

import dev.as_request.dto.AssignmentRequest;
import dev.as_request.exception.AssignmentFailedException;
import dev.as_request.exception.TechnicianServiceException;
import dev.as_request.service.TechnicianService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class TechnicianServiceTests {
    private MockRestServiceServer server;
    private TechnicianService service;
    private final LocalDate date = LocalDate.of(2026, 10, 21);
    private final String availableUrl = "http://technician.test/api/assignments/technicians/available"
            + "?region=SEOUL&productType=AIR_PURIFIER&date=2026-10-21&timeSlot=MORNING";

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://technician.test");
        server = MockRestServiceServer.bindTo(builder).build();
        service = new TechnicianService(builder.build());
    }

    @Test
    void wrapsTechnicianLookupHttpErrorAndPreservesCause() {
        server.expect(requestTo(availableUrl)).andRespond(withServerError());
        TechnicianServiceException error = assertThrows(TechnicianServiceException.class,
                () -> service.getAvailableTechnicians("SEOUL", "AIR_PURIFIER", date, "MORNING"));
        assertNotNull(error.getCause());
        assertTrue(error.getMessage().contains("기사 서버"));
        server.verify();
    }

    @Test
    void rejectsMissingTechnicianListBody() {
        server.expect(requestTo(availableUrl)).andRespond(withNoContent());
        assertThrows(TechnicianServiceException.class,
                () -> service.getAvailableTechnicians("SEOUL", "AIR_PURIFIER", date, "MORNING"));
        server.verify();
    }

    @Test
    void wrapsRejectedAssignmentAndPreservesCause() {
        server.expect(requestTo("http://technician.test/api/assignments")).andRespond(withBadRequest());
        AssignmentFailedException error = assertThrows(AssignmentFailedException.class,
                () -> service.assignTechnician(new AssignmentRequest(1L, 701L, date.atTime(9, 6), "MORNING")));
        assertNotNull(error.getCause());
        server.verify();
    }

    @Test
    void rejectsMissingAssignmentBody() {
        server.expect(requestTo("http://technician.test/api/assignments")).andRespond(withNoContent());
        assertThrows(AssignmentFailedException.class,
                () -> service.assignTechnician(new AssignmentRequest(1L, 701L, date.atTime(9, 6), "MORNING")));
        server.verify();
    }
}
