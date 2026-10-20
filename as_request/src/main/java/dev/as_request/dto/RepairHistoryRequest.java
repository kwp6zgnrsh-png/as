package dev.as_request.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RepairHistoryRequest(
        @NotBlank(message = "신청자명을 입력해 주세요.")
        @Size(min = 2, max = 8, message = "신청자명은 2자 이상 8자 이하로 입력해 주세요.")
        String customerName,

        @NotBlank(message = "연락처를 입력해 주세요.")
        @Pattern(regexp = "^010[0-9]{8}$",
                message = "연락처는 하이픈 없이 010으로 시작하는 11자리 숫자로 입력해 주세요.")
        String phone
) {
}
