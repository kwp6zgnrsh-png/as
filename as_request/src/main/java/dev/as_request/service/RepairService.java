package dev.as_request.service;

import dev.as_request.dto.CreateRepairRequest;
import dev.as_request.dto.RepairResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RepairService {

    // TODO: 접수 DB 연결 시 이 임시 저장소를 Repository로 교체합니다.
    // 서버를 재시작하면 접수 내역은 초기화됩니다.
    private final ConcurrentLinkedDeque<RepairResponse> repairs = new ConcurrentLinkedDeque<>();
    private final AtomicLong sequence = new AtomicLong();

    public RepairResponse createRepair(CreateRepairRequest request) {
        validate(request);
        // TODO: 제품 서버 연동 시 productId의 존재 여부를 API로 확인합니다.
        RepairResponse response = new RepairResponse(
                sequence.incrementAndGet(),
                request.productId(),
                request.customerName().trim(),
                request.phone(),
                request.region().trim(),
                request.visitDateTime(),
                request.memo() == null ? "" : request.memo().trim(),
                "RECEIVED",
                LocalDateTime.now()
        );
        repairs.addFirst(response);
        return response;
    }

    public List<RepairResponse> getRepairs() {
        return List.copyOf(repairs);
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
