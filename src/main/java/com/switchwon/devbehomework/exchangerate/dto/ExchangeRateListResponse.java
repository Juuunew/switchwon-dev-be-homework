package com.switchwon.devbehomework.exchangerate.dto;

import java.util.List;

public record ExchangeRateListResponse(List<ExchangeRateResponse> exchangeRateList) {

	public static ExchangeRateListResponse from(List<ExchangeRateResponse> rates) {
		return new ExchangeRateListResponse(rates);
	}
}
