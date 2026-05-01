package com.switchwon.devbehomework.exchangerate.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.switchwon.devbehomework.common.enums.CurrencyCode;
import com.switchwon.devbehomework.common.enums.ErrorCode;
import com.switchwon.devbehomework.common.exception.BusinessException;
import com.switchwon.devbehomework.exchangerate.dto.ExchangeRateListResponse;
import com.switchwon.devbehomework.exchangerate.dto.ExchangeRateResponse;
import com.switchwon.devbehomework.exchangerate.entity.ExchangeRate;
import com.switchwon.devbehomework.exchangerate.repository.ExchangeRateRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class ExchangeRateService {

	private final ExchangeRateRepository exchangeRateRepository;

	@Transactional
	public void saveRates(List<ExchangeRateResponse> rates) {
		List<ExchangeRate> entities = rates.stream()
			.map(rate -> ExchangeRate.of(rate.currencyCode(), rate.tradeStanRate(),
				rate.buyRate(), rate.sellRate(), rate.dateTime()))
			.toList();
		exchangeRateRepository.saveAll(entities);
	}

	public ExchangeRateListResponse getLatestRates() {
		List<ExchangeRate> entities = exchangeRateRepository.findLatestRatesForAllCurrencies();
		List<ExchangeRateResponse> responses = entities.stream()
			.map(ExchangeRateResponse::from)
			.toList();
		return ExchangeRateListResponse.from(responses);
	}

	public ExchangeRateResponse getLatestRate(CurrencyCode currency) {
		ExchangeRate entity = exchangeRateRepository
			.findTopByCurrencyOrderByCollectedAtDesc(currency)
			.orElseThrow(() -> new BusinessException(ErrorCode.EXCHANGE_RATE_NOT_FOUND));
		return ExchangeRateResponse.from(entity);
	}
}
