package com.koreaconcrete.civilshop.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiError> handleBusinessException(BusinessException exception) {
		return ResponseEntity
				.status(exception.getStatus())
				.body(ApiError.of(exception.getCode(), exception.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException exception) {
		Map<String, Object> details = new LinkedHashMap<>();
		exception.getBindingResult().getFieldErrors()
				.forEach(error -> details.put(error.getField(), error.getDefaultMessage()));
		return ResponseEntity.badRequest().body(new ApiError(
				"VALIDATION_ERROR",
				"입력값을 확인해주세요.",
				details
		));
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException exception) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN)
				.body(ApiError.of("ACCESS_DENIED", "접근 권한이 없습니다."));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiError> handleNoResourceFound(NoResourceFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(ApiError.of("NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."));
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public ResponseEntity<ApiError> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException exception) {
		return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
				.body(ApiError.of("UPLOAD_SIZE_EXCEEDED", "이미지는 8MB 이하로 업로드해주세요."));
	}

	@ExceptionHandler(MultipartException.class)
	public ResponseEntity<ApiError> handleMultipartException(MultipartException exception) {
		return ResponseEntity.badRequest()
				.body(ApiError.of("UPLOAD_REQUEST_ERROR", "이미지 업로드 요청을 확인해주세요."));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleException(Exception exception) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(ApiError.of("INTERNAL_SERVER_ERROR", "서버 처리 중 오류가 발생했습니다."));
	}
}
