package com.switchwon.devbehomework.exchangerate.service;

import com.switchwon.devbehomework.currency.Currency;
import com.switchwon.devbehomework.exchangerate.dto.ExchangeRateListResponse;
import com.switchwon.devbehomework.exchangerate.dto.ExchangeRateResponse;

public interface ExchangeRateService {

	ExchangeRateListResponse getLatestRates(Currency from);

	ExchangeRateResponse getLatestRate(Currency from, Currency to);
}
