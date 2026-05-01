package com.switchwon.devbehomework.common.exception;

import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.switchwon.devbehomework.common.dto.ApiResponse;
import com.switchwon.devbehomework.common.enums.ErrorCode;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
		ErrorCode errorCode = ex.getErrorCode();
		return ResponseEntity
			.status(errorCode.getCode())
			.body(ApiResponse.error(String.valueOf(errorCode.getCode()), errorCode.getMessage()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
			.findFirst()
			.map(DefaultMessageSourceResolvable::getDefaultMessage)
			.orElse("잘못된 요청입니다.");
		return ResponseEntity
			.badRequest()
			.body(ApiResponse.error("400", message));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
		HttpMessageNotReadableException ex) {
		return ResponseEntity
			.badRequest()
			.body(ApiResponse.error("400", "잘못된 요청 형식입니다."));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
		MethodArgumentTypeMismatchException ex) {
		return ResponseEntity
			.badRequest()
			.body(ApiResponse.error("400", "잘못된 요청 형식입니다."));
	}
}
