package com.switchwon.devbehomework.exchangerate.provider;

import java.util.List;

import com.switchwon.devbehomework.exchangerate.dto.ExchangeRateResponse;

public interface ExchangeRateProvider {

	List<ExchangeRateResponse> fetchRates();
}
