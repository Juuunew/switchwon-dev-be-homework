package com.switchwon.devbehomework.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	EXCHANGE_RATE_NOT_FOUND(404, "환율 정보를 찾을 수 없습니다."),
	INVALID_CURRENCY_PAIR(400, "KRW를 포함한 통화 쌍만 주문 가능합니다."),
	SAME_CURRENCY(400, "출발 통화와 도착 통화가 동일합니다."),
	EXTERNAL_API_ERROR(500, "외부 환율 API 호출에 실패했습니다."),
	INVALID_REQUEST(400, "잘못된 요청입니다."),
	RATE_STALE(503, "환율 정보가 만료되었습니다. 잠시 후 다시 시도해주세요."),
	UNSUPPORTED_CURRENCY(400, "지원하지 않는 통화입니다.");

	private final int code;
	private final String message;
}
