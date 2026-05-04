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

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
		ErrorCode errorCode = ex.getErrorCode();
		return ResponseEntity
			.status(errorCode.getCode())
			.body(ApiResponse.error(errorCode));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
			.findFirst()
			.map(DefaultMessageSourceResolvable::getDefaultMessage)
			.orElse("잘못된 요청입니다.");
		return ResponseEntity
			.badRequest()
			.body(ApiResponse.error(ErrorCode.INVALID_REQUEST, message));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
		HttpMessageNotReadableException ex) {
		return ResponseEntity
			.badRequest()
			.body(ApiResponse.error(ErrorCode.INVALID_REQUEST, "잘못된 요청 형식입니다."));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
		MethodArgumentTypeMismatchException ex) {
		return ResponseEntity
			.badRequest()
			.body(ApiResponse.error(ErrorCode.INVALID_REQUEST, "잘못된 요청 형식입니다."));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleUnhandledException(Exception ex) {
		log.error("처리되지 않은 예외가 발생했습니다.", ex);
		ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
		return ResponseEntity
			.status(errorCode.getCode())
			.body(ApiResponse.error(errorCode));
	}
}
