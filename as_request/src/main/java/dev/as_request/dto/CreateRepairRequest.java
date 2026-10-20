package dev.as_request.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record CreateRepairRequest(
	@NotNull(message = "제품 ID를 입력해 주세요.")
	@Positive(message = "제품 ID는 양수여야 합니다.")
	Long productId,

	@NotBlank(message = "신청자명을 입력해 주세요.")
	@Size(min = 2, max = 8, message = "신청자명은 2자 이상 8자 이하로 입력해 주세요.")
	String customerName,

	@NotBlank(message = "연락처를 입력해 주세요.")
	@Pattern(regexp = "^010[0-9]{8}$", message = "연락처는 하이픈 없이 010으로 시작하는 11자리 숫자로 입력해 주세요.")
	String phone,

	@NotBlank(message = "지역을 입력해 주세요.")
	@Pattern(regexp = "^(SEOUL|BUSAN)$", message = "지역은 seoul 또는 busan만 입력할 수 있습니다.")
	String region,

	@NotNull(message = "희망 방문 일시를 입력해 주세요.")
	@Future(message = "희망 방문 일시는 현재 이후여야 합니다.")
	LocalDateTime visitDateTime,

	@Size(max = 1000, message = "메모는 1000자 이하로 입력해 주세요.")
	String memo
){
        @AssertTrue(message="방문 시간은 오전 09:00~12:00 또는 오후 13:00~18:00이어야 합니다.")
        public boolean isVisitTimeValid() {
                if (visitDateTime == null) return true;

                LocalTime time = visitDateTime.toLocalTime();

                boolean morning = !time.isBefore(LocalTime.of(9, 0))
                    && time.isBefore(LocalTime.of(12, 0));

                boolean afternoon = !time.isBefore(LocalTime.of(13, 0))
                    && time.isBefore(LocalTime.of(18, 0));

                return morning || afternoon;
        }
}
