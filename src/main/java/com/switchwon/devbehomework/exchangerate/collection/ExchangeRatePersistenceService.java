package com.switchwon.devbehomework.exchangerate.collection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.switchwon.devbehomework.currency.Currency;
import com.switchwon.devbehomework.currency.RatedCurrency;
import com.switchwon.devbehomework.exchangerate.dto.ExchangeRateResponse;
import com.switchwon.devbehomework.exchangerate.entity.ExchangeRateHistoryEntity;
import com.switchwon.devbehomework.exchangerate.provider.ProviderRate;
import com.switchwon.devbehomework.exchangerate.repository.ExchangeRateRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class ExchangeRatePersistenceService {

	private final ExchangeRateRepository exchangeRateRepository;

	@Value("${exchange-rate.spread-buy:1.05}")
	private BigDecimal spreadBuy;

	@Value("${exchange-rate.spread-sell:0.95}")
	private BigDecimal spreadSell;

	@Value("${exchange-rate.sanity-check.warn-threshold:0.05}")
	private BigDecimal warnThreshold;

	@Value("${exchange-rate.sanity-check.skip-threshold:0.15}")
	private BigDecimal skipThreshold;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Optional<SavedExchangeRate> saveIfValid(ProviderRate providerRate, String providerName,
		LocalDateTime now) {
		RatedCurrency currency = providerRate.from();
		Currency toCurrency = providerRate.to();

		if (providerRate.unitRate() == null
			|| providerRate.unitRate().compareTo(BigDecimal.ZERO) <= 0) {
			log.warn("{}({}) unitRate 무효: {}", currency, providerName, providerRate.unitRate());
			return Optional.empty();
		}

		BigDecimal storedBaseRate = providerRate.unitRate()
			.multiply(BigDecimal.valueOf(currency.getRateUnit()))
			.setScale(2, RoundingMode.HALF_UP);

		if (!passesSanityCheck(currency, toCurrency, storedBaseRate)) {
			return Optional.empty();
		}

		BigDecimal buyRate = storedBaseRate.multiply(spreadBuy).setScale(2, RoundingMode.HALF_UP);
		BigDecimal sellRate = storedBaseRate.multiply(spreadSell).setScale(2, RoundingMode.HALF_UP);

		ExchangeRateHistoryEntity saved = exchangeRateRepository.save(ExchangeRateHistoryEntity.builder()
			.fromCurrency(currency)
			.toCurrency(toCurrency)
			.baseRate(storedBaseRate)
			.buyRate(buyRate)
			.sellRate(sellRate)
			.provider(providerName)
			.dateTime(now)
			.build());

		log.info("환율 저장 성공: {} → {} = {} (provider={})", currency, toCurrency, storedBaseRate, providerName);
		return Optional.of(new SavedExchangeRate(currency, ExchangeRateResponse.from(saved)));
	}

	private boolean passesSanityCheck(RatedCurrency currency, Currency toCurrency, BigDecimal newRate) {
		Optional<ExchangeRateHistoryEntity> prevOpt = exchangeRateRepository
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

		if (change.compareTo(skipThreshold) >= 0) {
			log.warn("환율 sanity check 초과 (SKIP): {} 변동={}% (이전={}, 신규={})",
				currency, changePct, prevRate, newRate);
			return false;
		}

		if (change.compareTo(warnThreshold) >= 0) {
			log.warn("환율 sanity check 경고: {} 변동={}% (이전={}, 신규={})",
				currency, changePct, prevRate, newRate);
		}

		return true;
	}
}
