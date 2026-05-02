package com.switchwon.devbehomework.exchangerate.provider;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;

/**
 * Provider 순서를 application.yml로 관리한다.
 * exchange-rate.collection.providers 설정값의 순서가 fallback 우선순위가 된다.
 */
@Configuration
@RequiredArgsConstructor
public class ProviderConfig {

	private final ExchangeRateApiProvider exchangeRateApiProvider;
	private final FrankfurterExchangeRateProvider frankfurterExchangeRateProvider;
	private final MockExchangeRateProvider mockExchangeRateProvider;

	@Value("${exchange-rate.collection.providers:EXCHANGE_RATE_API,FRANKFURTER,MOCK}")
	private String providers;

	@Bean
	public List<ExchangeRateProvider> orderedProviders() {
		List<ExchangeRateProvider> all = List.of(
			exchangeRateApiProvider, frankfurterExchangeRateProvider, mockExchangeRateProvider
		);
		return Arrays.stream(providers.split(","))
			.map(String::trim)
			.map(name -> findProvider(name, all))
			.toList();
	}

	private ExchangeRateProvider findProvider(String name, List<ExchangeRateProvider> all) {
		return all.stream()
			.filter(p -> p.getName().equals(name))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Unknown exchange rate provider: " + name));
	}
}
