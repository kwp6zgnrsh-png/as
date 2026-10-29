package dev.as_request.service;

import dev.as_request.dto.AssignmentRequest;
import dev.as_request.dto.AssignmentResponse;
import dev.as_request.dto.CreateRepairRequest;
import dev.as_request.dto.ProductResponse;
import dev.as_request.dto.RepairHistoryRequest;
import dev.as_request.dto.RepairListResponse;
import dev.as_request.dto.RepairResponse;
import dev.as_request.dto.TechnicianResponse;
import dev.as_request.exception.AssignmentFailedException;
import dev.as_request.exception.ProductServiceException;
import dev.as_request.exception.TechnicianNotFoundException;
import dev.as_request.repository.RepairRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class RepairServiceTest {

    private static final ProductResponse PRODUCT = new ProductResponse(101L, "공기청정기", "AIR_PURIFIER");
    private static final TechnicianResponse TECHNICIAN = new TechnicianResponse(701L, "김기사", "SEOUL", true);

    @Mock
    private RepairRepository repairRepository;
    @Mock
    private ProductService productService;
    @Mock
    private TechnicianService technicianService;
    @InjectMocks
    private RepairService repairService;

    @ParameterizedTest
    @CsvSource({"9, MORNING", "11, MORNING", "13, AFTERNOON", "17, AFTERNOON"})
    @DisplayName("방문 시간대에 가능한 첫 번째 기사를 배정하고 상태를 변경한다")
    void createsRepairAndAssignsFirstTechnician(int hour, String timeSlot) {
        // Given
        CreateRepairRequest request = requestAt(hour);
        given(productService.getProduct(101L)).willReturn(PRODUCT);
        given(technicianService.getAvailableTechnicians("SEOUL", "AIR_PURIFIER",
                request.visitDateTime().toLocalDate(), timeSlot))
                .willReturn(List.of(TECHNICIAN, new TechnicianResponse(702L, "이기사", "SEOUL", true)));
        given(repairRepository.save(request, 101L, 701L)).willReturn(savedRepair(request));
        AssignmentRequest assignment = new AssignmentRequest(1L, 701L, request.visitDateTime(), timeSlot);
        given(technicianService.assignTechnician(assignment)).willReturn(
                new AssignmentResponse(10L, 1L, 701L, "ASSIGNED", request.visitDateTime()));

        // When
        repairService.createRepair(request);

        // Then
        then(repairRepository).should().save(request, 101L, 701L);
        then(technicianService).should().assignTechnician(assignment);
        then(repairRepository).should().updateStatus(1L, "ASSIGNED");
    }

    @Test
    @DisplayName("제품 조회에 실패하면 기사 조회와 DB 저장을 수행하지 않는다")
    void stopsWhenProductLookupFails() {
        // Given
        CreateRepairRequest request = requestAt(10);
        ProductServiceException failure = new ProductServiceException("제품 조회 실패");
        given(productService.getProduct(101L)).willThrow(failure);

        // When
        Throwable thrown = catchThrowable(() -> repairService.createRepair(request));

        // Then
        assertThat(thrown).isSameAs(failure);
        then(technicianService).shouldHaveNoInteractions();
        then(repairRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("제품 정보가 없으면 접수를 중단한다")
    void rejectsMissingProduct() {
        // Given
        CreateRepairRequest request = requestAt(10);
        given(productService.getProduct(101L)).willReturn(null);

        // When
        Throwable thrown = catchThrowable(() -> repairService.createRepair(request));

        // Then
        assertThat(thrown).isInstanceOf(ProductServiceException.class);
        then(technicianService).shouldHaveNoInteractions();
        then(repairRepository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("배정 가능한 기사가 없으면 접수를 저장하지 않는다")
    void rejectsUnavailableTechnician() {
        // Given
        CreateRepairRequest request = requestAt(10);
        given(productService.getProduct(101L)).willReturn(PRODUCT);
        given(technicianService.getAvailableTechnicians("SEOUL", "AIR_PURIFIER",
                request.visitDateTime().toLocalDate(), "MORNING")).willReturn(List.of());

        // When
        Throwable thrown = catchThrowable(() -> repairService.createRepair(request));

        // Then
        assertThat(thrown).isInstanceOf(TechnicianNotFoundException.class);
        then(repairRepository).shouldHaveNoInteractions();
        then(technicianService).should(never()).assignTechnician(any());
    }

    @Test
    @DisplayName("접수 저장이 실패하면 기사 배정을 요청하지 않는다")
    void stopsWhenSavingFails() {
        // Given
        CreateRepairRequest request = requestAt(10);
        givenAvailableTechnician(request);
        DataAccessResourceFailureException failure = new DataAccessResourceFailureException("DB 연결 실패");
        given(repairRepository.save(request, 101L, 701L)).willThrow(failure);

        // When
        Throwable thrown = catchThrowable(() -> repairService.createRepair(request));

        // Then
        assertThat(thrown).isSameAs(failure);
        then(technicianService).should(never()).assignTechnician(any());
        then(repairRepository).should(never()).updateStatus(anyLong(), anyString());
    }

    @Test
    @DisplayName("기사 배정이 실패하면 ASSIGNED 상태로 변경하지 않는다")
    void doesNotUpdateStatusWhenAssignmentFails() {
        // Given
        CreateRepairRequest request = requestAt(10);
        givenAvailableTechnician(request);
        given(repairRepository.save(request, 101L, 701L)).willReturn(savedRepair(request));
        AssignmentFailedException failure = new AssignmentFailedException("기사 배정 실패");
        given(technicianService.assignTechnician(new AssignmentRequest(1L, 701L,
                request.visitDateTime(), "MORNING"))).willThrow(failure);

        // When
        Throwable thrown = catchThrowable(() -> repairService.createRepair(request));

        // Then
        assertThat(thrown).isSameAs(failure);
        then(repairRepository).should(never()).updateStatus(anyLong(), anyString());
    }

    @Test
    @DisplayName("기사 배정 응답이 없으면 예외를 발생시키고 상태를 변경하지 않는다")
    void rejectsMissingAssignmentResponse() {
        // Given
        CreateRepairRequest request = requestAt(10);
        givenAvailableTechnician(request);
        given(repairRepository.save(request, 101L, 701L)).willReturn(savedRepair(request));
        given(technicianService.assignTechnician(new AssignmentRequest(1L, 701L,
                request.visitDateTime(), "MORNING"))).willReturn(null);

        // When
        Throwable thrown = catchThrowable(() -> repairService.createRepair(request));

        // Then
        assertThat(thrown).isInstanceOf(AssignmentFailedException.class);
        then(repairRepository).should(never()).updateStatus(anyLong(), anyString());
    }

    @Test
    @DisplayName("이름과 전화번호로 본인의 접수 내역을 조회한다")
    void getsMyRepairs() {
        // Given
        RepairHistoryRequest request = new RepairHistoryRequest("홍길동", "01012345678");
        RepairListResponse repair = new RepairListResponse("AS-1", "홍길동", "01012345678",
                "SEOUL", "전원 불량", "ASSIGNED", requestAt(10).visitDateTime());
        given(repairRepository.findByCustomerNameAndPhone("홍길동", "01012345678")).willReturn(List.of(repair));

        // When
        List<RepairListResponse> result = repairService.getMyRepairs(request);

        // Then
        assertThat(result).containsExactly(repair);
        then(repairRepository).should().findByCustomerNameAndPhone("홍길동", "01012345678");
        then(repairRepository).should(never()).findAll();
    }

    @Test
    @DisplayName("전체 접수 내역을 반환한다")
    void getsAllRepairs() {
        // Given
        RepairListResponse repair = new RepairListResponse("AS-1", "홍길동", "01012345678",
                "SEOUL", null, "ASSIGNED", requestAt(10).visitDateTime());
        given(repairRepository.findAll()).willReturn(List.of(repair));

        // When
        List<RepairListResponse> result = repairService.getRepairs();

        // Then
        assertThat(result).containsExactly(repair);
        then(repairRepository).should().findAll();
    }

    private CreateRepairRequest requestAt(int hour) {
        return new CreateRepairRequest(101L, "홍길동", "01012345678", "SEOUL",
                LocalDateTime.of(2099, 1, 1, hour, 0), "전원 불량");
    }

    private RepairResponse savedRepair(CreateRepairRequest request) {
        return new RepairResponse(1L, "AS-1", request.customerName(), request.phone(), 101L,
                request.region(), request.memo(), 701L, "RECEIVED", request.visitDateTime());
    }

    private void givenAvailableTechnician(CreateRepairRequest request) {
        given(productService.getProduct(101L)).willReturn(PRODUCT);
        given(technicianService.getAvailableTechnicians("SEOUL", "AIR_PURIFIER",
                request.visitDateTime().toLocalDate(), "MORNING")).willReturn(List.of(TECHNICIAN));
    }
}
