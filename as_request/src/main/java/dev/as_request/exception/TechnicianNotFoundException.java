package dev.as_request.exception;

public class TechnicianNotFoundException extends RuntimeException {

    public TechnicianNotFoundException() {
        super("선택한 지역과 시간대에 배정 가능한 기사가 없습니다.");
    }
}
