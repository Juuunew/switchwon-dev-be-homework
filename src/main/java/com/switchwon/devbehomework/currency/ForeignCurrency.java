package com.switchwon.devbehomework.currency;

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
}
