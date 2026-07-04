package com.koreaconcrete.civilshop.common.exception;

import java.util.Map;

public record ApiError(
		String code,
		String message,
		Map<String, Object> details
) {
	public static ApiError of(String code, String message) {
		return new ApiError(code, message, Map.of());
	}
}
