package com.switchwon.devbehomework.exchangerate.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.switchwon.devbehomework.common.enums.CurrencyCode;
import com.switchwon.devbehomework.exchangerate.entity.ExchangeRate;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

	Optional<ExchangeRate> findTopByCurrencyOrderByCollectedAtDesc(CurrencyCode currency);

	@Query("SELECT er FROM ExchangeRate er "
		+ "WHERE er.collectedAt = ("
		+ "  SELECT MAX(er2.collectedAt) FROM ExchangeRate er2 "
		+ "  WHERE er2.currency = er.currency"
		+ ")")
	List<ExchangeRate> findLatestRatesForAllCurrencies();
}
