package com.switchwon.devbehomework.exchangerate.collection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.switchwon.devbehomework.currency.CurrencyCode;
import com.switchwon.devbehomework.currency.ForeignCurrency;
import com.switchwon.devbehomework.exchangerate.entity.ExchangeRateEntity;
import com.switchwon.devbehomework.exchangerate.provider.ExchangeRateProvider;
import com.switchwon.devbehomework.exchangerate.provider.ProviderRate;
import com.switchwon.devbehomework.exchangerate.repository.ExchangeRateRepository;

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

	private static final BigDecimal SPREAD_BUY = new BigDecimal("1.05");
	private static final BigDecimal SPREAD_SELL = new BigDecimal("0.95");
	private static final BigDecimal WARN_THRESHOLD = new BigDecimal("0.05");
	private static final BigDecimal SKIP_THRESHOLD = new BigDecimal("0.15");

	private final List<ExchangeRateProvider> orderedProviders;
	private final ExchangeRateRepository exchangeRateRepository;
	private final Clock clock;

	@Override
	@Transactional
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

				if (providerRate.unitRate() == null
					|| providerRate.unitRate().compareTo(BigDecimal.ZERO) <= 0) {
					log.warn("{}({}) unitRate 무효: {}", currency, provider.getName(), providerRate.unitRate());
					continue;
				}

				BigDecimal storedBaseRate = providerRate.unitRate()
					.multiply(BigDecimal.valueOf(currency.getRateUnit()))
					.setScale(2, RoundingMode.HALF_UP);

				if (!passesSanityCheck(currency, storedBaseRate)) {
					continue;
				}

				BigDecimal buyRate = storedBaseRate.multiply(SPREAD_BUY).setScale(2, RoundingMode.HALF_UP);
				BigDecimal sellRate = storedBaseRate.multiply(SPREAD_SELL).setScale(2, RoundingMode.HALF_UP);

				exchangeRateRepository.save(ExchangeRateEntity.builder()
					.fromCurrency(currency)
					.toCurrency(CurrencyCode.KRW)
					.baseRate(storedBaseRate)
					.buyRate(buyRate)
					.sellRate(sellRate)
					.provider(provider.getName())
					.dateTime(now)
					.build());

				log.info("환율 수집 성공: {} → KRW = {} (provider={})", currency, storedBaseRate, provider.getName());
				return;

			} catch (Exception ex) {
				log.warn("{}({}) 환율 수집 실패, 다음 provider 시도: {}", currency, provider.getName(), ex.getMessage());
			}
		}
		log.warn("모든 provider 실패: {} 환율 수집 불가", currency);
	}

	private boolean passesSanityCheck(ForeignCurrency currency, BigDecimal newRate) {
		Optional<ExchangeRateEntity> prevOpt = exchangeRateRepository
			.findTopByFromCurrencyAndToCurrencyOrderByDateTimeDesc(currency, CurrencyCode.KRW);

		if (prevOpt.isEmpty()) {
			return true;
		}

		BigDecimal prevRate = prevOpt.get().getBaseRate();
		if (prevRate.compareTo(BigDecimal.ZERO) == 0) {
			return true;
		}

		BigDecimal change = newRate.subtract(prevRate).abs()
			.divide(prevRate, 4, RoundingMode.HALF_UP);

		BigDecimal changePct = change.multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);

		if (change.compareTo(SKIP_THRESHOLD) >= 0) {
			log.warn("환율 sanity check 초과 (SKIP): {} 변동={}% (이전={}, 신규={})",
				currency, changePct, prevRate, newRate);
			return false;
		}

		if (change.compareTo(WARN_THRESHOLD) >= 0) {
			log.warn("환율 sanity check 경고: {} 변동={}% (이전={}, 신규={})",
				currency, changePct, prevRate, newRate);
		}

		return true;
	}
}
