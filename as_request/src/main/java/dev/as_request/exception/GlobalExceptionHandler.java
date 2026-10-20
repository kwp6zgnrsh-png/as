package dev.as_request.exception;

import org.springframework.dao.DataAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, String> handleValidationException(MethodArgumentNotValidException exception) {
		String message = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> error.getDefaultMessage())
			.findFirst()
			.orElse("입력값을 확인해 주세요.");
		return Map.of("message", message);
	}

	@ExceptionHandler({ProductServiceException.class, TechnicianServiceException.class,
		AssignmentFailedException.class})
	@ResponseStatus(HttpStatus.BAD_GATEWAY)
	public Map<String, String> handleExternalServiceException(RuntimeException exception) {
		return Map.of("message", exception.getMessage());
	}

	@ExceptionHandler(TechnicianNotFoundException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public Map<String, String> handleTechnicianNotFound(TechnicianNotFoundException exception) {
		return Map.of("message", exception.getMessage());
	}

	@ExceptionHandler(RestClientException.class)
	@ResponseStatus(HttpStatus.BAD_GATEWAY)
	public Map<String, String> handleRestClientException() {
		return Map.of("message", "외부 서버와 통신하지 못했습니다. 잠시 후 다시 시도해 주세요.");
	}

	@ExceptionHandler(DataAccessException.class)
	@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
	public Map<String, String> handleDataAccessException() {
		return Map.of("message", "접수 목록을 조회할 수 없습니다. DB 연결 및 설정을 확인해 주세요.");
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, String> handleUnreadableRequest() {
		return Map.of("message", "요청 JSON 형식을 확인해 주세요.");
	}
}
