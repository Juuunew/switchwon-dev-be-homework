package com.switchwon.devbehomework.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CurrencyCode {

	KRW(null),
	USD(ForeignCurrency.USD),
	JPY(ForeignCurrency.JPY),
	CNY(ForeignCurrency.CNY),
	EUR(ForeignCurrency.EUR);

	private final ForeignCurrency foreignCurrency;
}
