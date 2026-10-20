package dev.as_request.global.exception;

import org.springframework.dao.DataAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RestClientException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public Map<String, String> handleProductServiceException() {
        return Map.of("message", "제품 서버에서 정보를 가져오지 못했습니다. 잠시 후 다시 시도해 주세요.");
    }

    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, String> handleDataAccessException() {
        return Map.of("message", "접수 목록을 조회할 수 없습니다. DB 연결 및 설정을 확인해 주세요.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidRequest(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleUnreadableRequest() {
        return Map.of("message", "요청 JSON 형식을 확인해 주세요. 방문 일시는 yyyy-MM-dd'T'HH:mm 형식입니다.");
    }
}
