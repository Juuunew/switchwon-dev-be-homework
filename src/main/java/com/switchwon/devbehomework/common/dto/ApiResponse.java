package com.switchwon.devbehomework.common.dto;

import com.switchwon.devbehomework.common.enums.ErrorCode;

public record ApiResponse<T>(String code, String message, T returnObject) {

	public static <T> ApiResponse<T> success(T returnObject) {
		return new ApiResponse<>("OK", "정상적으로 처리되었습니다.", returnObject);
	}

	public static <T> ApiResponse<T> error(ErrorCode errorCode) {
		return error(errorCode, errorCode.getMessage());
	}

	public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
		return new ApiResponse<>(String.valueOf(errorCode.getCode()), message, null);
	}
}
