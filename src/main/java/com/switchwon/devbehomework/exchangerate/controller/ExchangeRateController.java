package com.switchwon.devbehomework.exchangerate.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.switchwon.devbehomework.common.dto.ApiResponse;
import com.switchwon.devbehomework.currency.Currency;
import com.switchwon.devbehomework.currency.RatedCurrency;
import com.switchwon.devbehomework.exchangerate.dto.ExchangeRateListResponse;
import com.switchwon.devbehomework.exchangerate.dto.ExchangeRateResponse;
import com.switchwon.devbehomework.exchangerate.service.ExchangeRateService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/exchange-rate")
@RestController
public class ExchangeRateController {

	private final ExchangeRateService exchangeRateService;

	@GetMapping("/latest")
	public ApiResponse<ExchangeRateListResponse> getLatestRates() {
		return ApiResponse.success(exchangeRateService.getLatestRates(Currency.KRW));
	}

	@GetMapping("/latest/{currency}")
	public ApiResponse<ExchangeRateResponse> getLatestRate(@PathVariable RatedCurrency currency) {
		return ApiResponse.success(exchangeRateService.getLatestRate(Currency.KRW, Currency.valueOf(currency.name())));
	}
}
