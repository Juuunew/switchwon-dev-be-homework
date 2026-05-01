package com.switchwon.devbehomework.scheduler;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.switchwon.devbehomework.exchangerate.dto.ExchangeRateResponse;
import com.switchwon.devbehomework.exchangerate.provider.ExchangeRateProvider;
import com.switchwon.devbehomework.exchangerate.service.ExchangeRateService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class ExchangeRateScheduler {

	private final ExchangeRateProvider exchangeRateProvider;
	private final ExchangeRateService exchangeRateService;

	@Scheduled(fixedRateString = "${scheduler.exchange-rate.fixed-rate}",
		initialDelayString = "${scheduler.exchange-rate.initial-delay}")
	public void collectExchangeRates() {
		try {
			List<ExchangeRateResponse> rates = exchangeRateProvider.fetchRates();
			exchangeRateService.saveRates(rates);
			log.info("Exchange rates collected successfully. {} currencies saved.", rates.size());
		} catch (Exception ex) {
			log.error("Failed to collect exchange rates", ex);
		}
	}
}
