package com.switchwon.devbehomework.currency;

import com.switchwon.devbehomework.common.enums.ErrorCode;
import com.switchwon.devbehomework.common.exception.BusinessException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ForeignCurrency {

	USD(1),
	JPY(100),
	CNY(1),
	EUR(1);

	private final int rateUnit;

	public static ForeignCurrency from(CurrencyCode code) {
		return switch (code) {
			case USD -> USD;
			case JPY -> JPY;
			case CNY -> CNY;
			case EUR -> EUR;
			case KRW -> throw new BusinessException(ErrorCode.INVALID_CURRENCY_PAIR);
		};
	}
}
