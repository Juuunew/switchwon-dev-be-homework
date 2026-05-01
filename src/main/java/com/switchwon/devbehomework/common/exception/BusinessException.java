package com.switchwon.devbehomework.common.exception;

import com.switchwon.devbehomework.common.enums.ErrorCode;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}
}
