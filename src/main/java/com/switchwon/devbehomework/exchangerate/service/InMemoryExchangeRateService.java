package com.switchwon.devbehomework.exchangerate.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.switchwon.devbehomework.common.enums.ErrorCode;
import com.switchwon.devbehomework.common.exception.BusinessException;
import com.switchwon.devbehomework.currency.Currency;
import com.switchwon.devbehomework.currency.RatedCurrency;
import com.switchwon.devbehomework.exchangerate.dto.ExchangeRateListResponse;
import com.switchwon.devbehomework.exchangerate.dto.ExchangeRateResponse;

@Primary
@Service
public class InMemoryExchangeRateService implements ExchangeRateService {

	private final Map<RatedCurrency, ExchangeRateResponse> cache = new ConcurrentHashMap<>();

	public void update(RatedCurrency currency, ExchangeRateResponse response) {
		cache.put(currency, response);
	}

	@Override
	public ExchangeRateListResponse getLatestRates(Currency from) {
		List<ExchangeRateResponse> rates = Arrays.stream(RatedCurrency.values())
			.filter(cache::containsKey)
			.map(cache::get)
			.toList();
		return ExchangeRateListResponse.from(rates);
	}

	@Override
	public ExchangeRateResponse getLatestRate(Currency from, Currency to) {
		RatedCurrency ratedTo;
		try {
			ratedTo = RatedCurrency.valueOf(to.name());
		} catch (IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.UNSUPPORTED_CURRENCY);
		}
		ExchangeRateResponse response = cache.get(ratedTo);
		if (response == null) {
			throw new BusinessException(ErrorCode.EXCHANGE_RATE_NOT_FOUND);
		}
		return response;
	}
}
