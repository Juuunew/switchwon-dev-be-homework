package com.switchwon.devbehomework.exchangerate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.switchwon.devbehomework.common.enums.ErrorCode;
import com.switchwon.devbehomework.common.exception.BusinessException;
import com.switchwon.devbehomework.currency.CurrencyCode;
import com.switchwon.devbehomework.currency.ForeignCurrency;
import com.switchwon.devbehomework.exchangerate.dto.ExchangeRateListResponse;
import com.switchwon.devbehomework.exchangerate.dto.ExchangeRateResponse;
import com.switchwon.devbehomework.exchangerate.entity.ExchangeRateEntity;
import com.switchwon.devbehomework.exchangerate.repository.ExchangeRateRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateService 단위 테스트")
class ExchangeRateServiceTest {

	@InjectMocks
	private ExchangeRateService exchangeRateService;

	@Mock
	private ExchangeRateRepository exchangeRateRepository;

	@Nested
	@DisplayName("getLatestRates 메서드")
	class GetLatestRates {

		@Test
		@DisplayName("모든 통화의 최신 환율 목록을 반환한다")
		void shouldReturnLatestRatesForAllCurrencies() {
			// given
			LocalDateTime now = LocalDateTime.now();
			List<ExchangeRateEntity> entities = List.of(
				ExchangeRateEntity.builder()
					.fromCurrency(ForeignCurrency.USD)
					.toCurrency(CurrencyCode.KRW)
					.baseRate(new BigDecimal("1350.00"))
					.buyRate(new BigDecimal("1417.50"))
					.sellRate(new BigDecimal("1282.50"))
					.provider("MOCK")
					.dateTime(now)
					.build(),
				ExchangeRateEntity.builder()
					.fromCurrency(ForeignCurrency.JPY)
					.toCurrency(CurrencyCode.KRW)
					.baseRate(new BigDecimal("900.00"))
					.buyRate(new BigDecimal("945.00"))
					.sellRate(new BigDecimal("855.00"))
					.provider("MOCK")
					.dateTime(now)
					.build()
			);
			given(exchangeRateRepository.findLatestRatesForAllCurrencies()).willReturn(entities);

			// when
			ExchangeRateListResponse response = exchangeRateService.getLatestRates();

			// then
			assertThat(response.exchangeRateList()).hasSize(2);
			assertThat(response.exchangeRateList().get(0).currencyCode()).isEqualTo(ForeignCurrency.USD);
			assertThat(response.exchangeRateList().get(1).currencyCode()).isEqualTo(ForeignCurrency.JPY);
		}

		@Test
		@DisplayName("저장된 환율이 없으면 빈 목록을 반환한다")
		void shouldReturnEmptyListWhenNoRatesExist() {
			// given
			given(exchangeRateRepository.findLatestRatesForAllCurrencies()).willReturn(List.of());

			// when
			ExchangeRateListResponse response = exchangeRateService.getLatestRates();

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
			ExchangeRateEntity entity = ExchangeRateEntity.builder()
				.fromCurrency(ForeignCurrency.USD)
				.toCurrency(CurrencyCode.KRW)
				.baseRate(new BigDecimal("1350.00"))
				.buyRate(new BigDecimal("1417.50"))
				.sellRate(new BigDecimal("1282.50"))
				.provider("MOCK")
				.dateTime(now)
				.build();
			given(exchangeRateRepository.findTopByFromCurrencyAndToCurrencyOrderByDateTimeDesc(
				ForeignCurrency.USD, CurrencyCode.KRW)).willReturn(Optional.of(entity));

			// when
			ExchangeRateResponse response = exchangeRateService.getLatestRate(ForeignCurrency.USD);

			// then
			assertThat(response.currencyCode()).isEqualTo(ForeignCurrency.USD);
			assertThat(response.tradeStanRate()).isEqualByComparingTo(new BigDecimal("1350.00"));
			assertThat(response.buyRate()).isEqualByComparingTo(new BigDecimal("1417.50"));
			assertThat(response.sellRate()).isEqualByComparingTo(new BigDecimal("1282.50"));
			assertThat(response.dateTime()).isEqualTo(now);
		}

		@Test
		@DisplayName("환율 정보가 없으면 BusinessException이 발생한다")
		void shouldThrowExceptionWhenRateNotFound() {
			// given
			given(exchangeRateRepository.findTopByFromCurrencyAndToCurrencyOrderByDateTimeDesc(
				ForeignCurrency.CNY, CurrencyCode.KRW)).willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> exchangeRateService.getLatestRate(ForeignCurrency.CNY))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(ErrorCode.EXCHANGE_RATE_NOT_FOUND);
		}
	}
}
