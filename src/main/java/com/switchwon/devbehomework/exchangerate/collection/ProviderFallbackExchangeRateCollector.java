package com.switchwon.devbehomework.exchangerate.collection;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.switchwon.devbehomework.currency.CurrencyCode;
import com.switchwon.devbehomework.currency.ForeignCurrency;
import com.switchwon.devbehomework.exchangerate.provider.ExchangeRateProvider;
import com.switchwon.devbehomework.exchangerate.provider.ProviderRate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 설정된 Provider 순서대로 각 통화쌍의 환율을 수집한다.
 * 한 Provider 실패 시 다음 Provider로 fallback하며, 통화별로 독립적으로 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProviderFallbackExchangeRateCollector implements ExchangeRateCollector {

	private final List<ExchangeRateProvider> orderedProviders;
	private final ExchangeRatePersistenceService persistenceService;
	private final Clock clock;

	@Override
	public void collectAll() {
		LocalDateTime now = LocalDateTime.now(clock);
		for (ForeignCurrency currency : ForeignCurrency.values()) {
			collectOne(currency, now);
		}
	}

	private void collectOne(ForeignCurrency currency, LocalDateTime now) {
		for (ExchangeRateProvider provider : orderedProviders) {
			if (!provider.supports(currency, CurrencyCode.KRW)) {
				continue;
			}
			try {
				ProviderRate providerRate = provider.fetchRate(currency, CurrencyCode.KRW);
				if (persistenceService.saveIfValid(providerRate, provider.getName(), now)) {
					return;
				}

			} catch (Exception ex) {
				log.warn("{}({}) 환율 수집 실패, 다음 provider 시도: {}", currency, provider.getName(), ex.getMessage());
			}
		}
		log.warn("모든 provider 실패: {} 환율 수집 불가", currency);
	}
}
