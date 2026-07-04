package com.koreaconcrete.civilshop.common.exception;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
	private final String code;
	private final HttpStatus status;

	public BusinessException(String code, String message, HttpStatus status) {
		super(message);
		this.code = code;
		this.status = status;
	}

	public static BusinessException notFound(String message) {
		return new BusinessException("NOT_FOUND", message, HttpStatus.NOT_FOUND);
	}

	public static BusinessException badRequest(String message) {
		return new BusinessException("BAD_REQUEST", message, HttpStatus.BAD_REQUEST);
	}

	public static BusinessException unauthorized(String message) {
		return new BusinessException("UNAUTHORIZED", message, HttpStatus.UNAUTHORIZED);
	}

	public static BusinessException forbidden(String message) {
		return new BusinessException("ACCESS_DENIED", message, HttpStatus.FORBIDDEN);
	}

	public String getCode() {
		return code;
	}

	public HttpStatus getStatus() {
		return status;
	}
}
