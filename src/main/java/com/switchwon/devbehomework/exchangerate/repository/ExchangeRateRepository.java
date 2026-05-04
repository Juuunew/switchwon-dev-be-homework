package com.switchwon.devbehomework.exchangerate.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.switchwon.devbehomework.currency.Currency;
import com.switchwon.devbehomework.currency.RatedCurrency;
import com.switchwon.devbehomework.exchangerate.entity.ExchangeRateHistoryEntity;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRateHistoryEntity, Long> {

	Optional<ExchangeRateHistoryEntity> findTopByFromCurrencyAndToCurrencyOrderByDateTimeDesc(
		RatedCurrency fromCurrency, Currency toCurrency);

	@Query("SELECT er FROM ExchangeRateHistoryEntity er "
		+ "WHERE er.dateTime = ("
		+ "  SELECT MAX(er2.dateTime) FROM ExchangeRateHistoryEntity er2 "
		+ "  WHERE er2.fromCurrency = er.fromCurrency AND er2.toCurrency = er.toCurrency"
		+ ")")
	List<ExchangeRateHistoryEntity> findLatestRatesForAllCurrencies();
}
