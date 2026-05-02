package com.switchwon.devbehomework.exchangerate.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.switchwon.devbehomework.currency.CurrencyCode;
import com.switchwon.devbehomework.currency.ForeignCurrency;
import com.switchwon.devbehomework.exchangerate.entity.ExchangeRateEntity;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRateEntity, Long> {

	Optional<ExchangeRateEntity> findTopByFromCurrencyAndToCurrencyOrderByDateTimeDesc(
		ForeignCurrency fromCurrency, CurrencyCode toCurrency);

	@Query("SELECT er FROM ExchangeRateEntity er "
		+ "WHERE er.dateTime = ("
		+ "  SELECT MAX(er2.dateTime) FROM ExchangeRateEntity er2 "
		+ "  WHERE er2.fromCurrency = er.fromCurrency AND er2.toCurrency = er.toCurrency"
		+ ")")
	List<ExchangeRateEntity> findLatestRatesForAllCurrencies();
}
