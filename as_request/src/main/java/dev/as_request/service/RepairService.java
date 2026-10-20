package dev.as_request.service;

import dev.as_request.dto.CreateRepairRequest;
import dev.as_request.dto.RepairResponse;
import dev.as_request.repository.RepairRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RepairService {

    private final RepairRepository repairRepository;
    private final ProductService productService;
    // POST는 아직 DB에 저장하지 않는 임시 응답입니다.
    private final AtomicLong sequence = new AtomicLong();

    public RepairService(RepairRepository repairRepository, ProductService productService) {
        this.repairRepository = repairRepository;
        this.productService = productService;
    }

    public RepairResponse createRepair(CreateRepairRequest request) {
        validate(request);
        productService.getProduct(request.productId());
        RepairResponse response = new RepairResponse(
                sequence.incrementAndGet(),
                null,
                request.customerName().trim(),
                request.phone(),
                request.productId(),
                request.region().trim(),
                request.memo() == null ? "" : request.memo().trim(),
                null,
                "RECEIVED",
                LocalDateTime.now()
        );
        return response;
    }

    public List<RepairResponse> getRepairs() {
        return repairRepository.findAll();
    }

    private void validate(CreateRepairRequest request) {
        if (request == null || request.productId() == null || request.productId() <= 0) {
            throw new IllegalArgumentException("제품 ID는 양수여야 합니다.");
        }
        if (request.customerName() == null || request.customerName().isBlank()
                || request.customerName().length() > 50) {
            throw new IllegalArgumentException("신청자명을 1~50자로 입력해 주세요.");
        }
        if (request.phone() == null || !request.phone().matches("0[0-9]{8,10}")) {
            throw new IllegalArgumentException("연락처는 0으로 시작하는 9~11자리 숫자로 입력해 주세요.");
        }
        if (request.region() == null || request.region().isBlank() || request.region().length() > 50) {
            throw new IllegalArgumentException("시·도를 1~50자로 입력해 주세요.");
        }
        if (request.visitDateTime() == null || !request.visitDateTime().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("희망 방문 일시는 현재 시간 이후로 선택해 주세요.");
        }
        if (request.memo() != null && request.memo().length() > 2000) {
            throw new IllegalArgumentException("메모사항은 2000자 이내로 입력해 주세요.");
        }
    }
}
