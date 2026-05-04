package com.switchwon.devbehomework.exchangerate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.switchwon.devbehomework.common.enums.ErrorCode;
import com.switchwon.devbehomework.common.exception.BusinessException;
import com.switchwon.devbehomework.currency.Currency;
import com.switchwon.devbehomework.currency.RatedCurrency;
import com.switchwon.devbehomework.exchangerate.dto.ExchangeRateListResponse;
import com.switchwon.devbehomework.exchangerate.dto.ExchangeRateResponse;

@DisplayName("InMemoryExchangeRateService 단위 테스트")
class InMemoryExchangeRateServiceTest {

	private InMemoryExchangeRateService exchangeRateService;

	@BeforeEach
	void setUp() {
		exchangeRateService = new InMemoryExchangeRateService();
	}

	@Nested
	@DisplayName("getLatestRates 메서드")
	class GetLatestRates {

		@Test
		@DisplayName("모든 통화의 최신 환율 목록을 반환한다")
		void shouldReturnLatestRatesForAllCurrencies() {
			// given
			LocalDateTime now = LocalDateTime.now();
			exchangeRateService.update(RatedCurrency.USD, ExchangeRateResponse.builder()
				.currencyCode(RatedCurrency.USD)
				.tradeStanRate(new BigDecimal("1350.00"))
				.buyRate(new BigDecimal("1417.50"))
				.sellRate(new BigDecimal("1282.50"))
				.dateTime(now)
				.build());
			exchangeRateService.update(RatedCurrency.JPY, ExchangeRateResponse.builder()
				.currencyCode(RatedCurrency.JPY)
				.tradeStanRate(new BigDecimal("900.00"))
				.buyRate(new BigDecimal("945.00"))
				.sellRate(new BigDecimal("855.00"))
				.dateTime(now)
				.build());

			// when
			ExchangeRateListResponse response = exchangeRateService.getLatestRates(Currency.KRW);

			// then
			assertThat(response.exchangeRateList()).hasSize(2);
		}

		@Test
		@DisplayName("저장된 환율이 없으면 빈 목록을 반환한다")
		void shouldReturnEmptyListWhenNoRatesExist() {
			// when
			ExchangeRateListResponse response = exchangeRateService.getLatestRates(Currency.KRW);

			// then
			assertThat(response.exchangeRateList()).isEmpty();
		}
	}

	@Nested
	@DisplayName("getLatestRate 메서드")
	class GetLatestRate {

		@Test
		@DisplayName("특정 통화의 최신 환율을 반환한다")
		void shouldReturnLatestRateForCurrency() {
			// given
			LocalDateTime now = LocalDateTime.now();
			exchangeRateService.update(RatedCurrency.USD, ExchangeRateResponse.builder()
				.currencyCode(RatedCurrency.USD)
				.tradeStanRate(new BigDecimal("1350.00"))
				.buyRate(new BigDecimal("1417.50"))
				.sellRate(new BigDecimal("1282.50"))
				.dateTime(now)
				.build());

			// when
			ExchangeRateResponse response = exchangeRateService.getLatestRate(Currency.KRW, Currency.USD);

			// then
			assertThat(response.currencyCode()).isEqualTo(RatedCurrency.USD);
			assertThat(response.tradeStanRate()).isEqualByComparingTo(new BigDecimal("1350.00"));
			assertThat(response.buyRate()).isEqualByComparingTo(new BigDecimal("1417.50"));
			assertThat(response.sellRate()).isEqualByComparingTo(new BigDecimal("1282.50"));
			assertThat(response.dateTime()).isEqualTo(now);
		}

		@Test
		@DisplayName("환율 정보가 없으면 BusinessException이 발생한다")
		void shouldThrowExceptionWhenRateNotFound() {
			// when & then
			assertThatThrownBy(() -> exchangeRateService.getLatestRate(Currency.KRW, Currency.CNY))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(ErrorCode.EXCHANGE_RATE_NOT_FOUND);
		}
	}
}
