package com.switchwon.devbehomework.exchangerate.collection;

import com.switchwon.devbehomework.currency.RatedCurrency;
import com.switchwon.devbehomework.exchangerate.dto.ExchangeRateResponse;

public record SavedExchangeRate(
	RatedCurrency currency,
	ExchangeRateResponse response
) {
}
