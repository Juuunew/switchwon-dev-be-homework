package com.switchwon.devbehomework.common.dto;

public record ApiResponse<T>(String code, String message, T returnObject) {

	public static <T> ApiResponse<T> success(T returnObject) {
		return new ApiResponse<>("OK", "정상적으로 처리되었습니다.", returnObject);
	}

	public static <T> ApiResponse<T> error(String code, String message) {
		return new ApiResponse<>(code, message, null);
	}
}
