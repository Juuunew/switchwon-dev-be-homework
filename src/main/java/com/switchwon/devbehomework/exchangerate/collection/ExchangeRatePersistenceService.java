package com.switchwon.devbehomework.exchangerate.collection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.switchwon.devbehomework.currency.CurrencyCode;
import com.switchwon.devbehomework.currency.ForeignCurrency;
import com.switchwon.devbehomework.exchangerate.entity.ExchangeRateEntity;
import com.switchwon.devbehomework.exchangerate.provider.ProviderRate;
import com.switchwon.devbehomework.exchangerate.repository.ExchangeRateRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class ExchangeRatePersistenceService {

	private static final BigDecimal SPREAD_BUY = new BigDecimal("1.05");
	private static final BigDecimal SPREAD_SELL = new BigDecimal("0.95");
	private static final BigDecimal WARN_THRESHOLD = new BigDecimal("0.05");
	private static final BigDecimal SKIP_THRESHOLD = new BigDecimal("0.15");

	private final ExchangeRateRepository exchangeRateRepository;

	@Transactional
	public boolean saveIfValid(ProviderRate providerRate, String providerName, LocalDateTime now) {
		ForeignCurrency currency = providerRate.from();
		CurrencyCode toCurrency = providerRate.to();

		if (providerRate.unitRate() == null
			|| providerRate.unitRate().compareTo(BigDecimal.ZERO) <= 0) {
			log.warn("{}({}) unitRate 무효: {}", currency, providerName, providerRate.unitRate());
			return false;
		}

		BigDecimal storedBaseRate = providerRate.unitRate()
			.multiply(BigDecimal.valueOf(currency.getRateUnit()))
			.setScale(2, RoundingMode.HALF_UP);

		if (!passesSanityCheck(currency, toCurrency, storedBaseRate)) {
			return false;
		}

		BigDecimal buyRate = storedBaseRate.multiply(SPREAD_BUY).setScale(2, RoundingMode.HALF_UP);
		BigDecimal sellRate = storedBaseRate.multiply(SPREAD_SELL).setScale(2, RoundingMode.HALF_UP);

		exchangeRateRepository.save(ExchangeRateEntity.builder()
			.fromCurrency(currency)
			.toCurrency(toCurrency)
			.baseRate(storedBaseRate)
			.buyRate(buyRate)
			.sellRate(sellRate)
			.provider(providerName)
			.dateTime(now)
			.build());

		log.info("환율 저장 성공: {} → {} = {} (provider={})", currency, toCurrency, storedBaseRate, providerName);
		return true;
	}

	private boolean passesSanityCheck(ForeignCurrency currency, CurrencyCode toCurrency, BigDecimal newRate) {
		Optional<ExchangeRateEntity> prevOpt = exchangeRateRepository
			.findTopByFromCurrencyAndToCurrencyOrderByDateTimeDesc(currency, toCurrency);

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
