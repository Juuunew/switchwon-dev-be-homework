package com.switchwon.devbehomework.exchangerate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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

import com.switchwon.devbehomework.common.enums.CurrencyCode;
import com.switchwon.devbehomework.common.enums.ErrorCode;
import com.switchwon.devbehomework.common.exception.BusinessException;
import com.switchwon.devbehomework.exchangerate.dto.ExchangeRateListResponse;
import com.switchwon.devbehomework.exchangerate.dto.ExchangeRateResponse;
import com.switchwon.devbehomework.exchangerate.entity.ExchangeRate;
import com.switchwon.devbehomework.exchangerate.repository.ExchangeRateRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateService 단위 테스트")
class ExchangeRateServiceTest {

	@InjectMocks
	private ExchangeRateService exchangeRateService;

	@Mock
	private ExchangeRateRepository exchangeRateRepository;

	@Nested
	@DisplayName("saveRates 메서드")
	class SaveRates {

		@Test
		@DisplayName("환율 목록을 전달하면 모든 환율이 저장된다")
		void shouldSaveAllRates() {
			// given
			LocalDateTime now = LocalDateTime.now();
			List<ExchangeRateResponse> rates = List.of(
				ExchangeRateResponse.builder()
					.currencyCode(CurrencyCode.USD)
					.tradeStanRate(new BigDecimal("1350.00"))
					.buyRate(new BigDecimal("1417.50"))
					.sellRate(new BigDecimal("1282.50"))
					.dateTime(now)
					.build(),
				ExchangeRateResponse.builder()
					.currencyCode(CurrencyCode.EUR)
					.tradeStanRate(new BigDecimal("1470.00"))
					.buyRate(new BigDecimal("1543.50"))
					.sellRate(new BigDecimal("1396.50"))
					.dateTime(now)
					.build()
			);

			// when
			exchangeRateService.saveRates(rates);

			// then
			verify(exchangeRateRepository).saveAll(anyList());
		}
	}

	@Nested
	@DisplayName("getLatestRates 메서드")
	class GetLatestRates {

		@Test
		@DisplayName("모든 통화의 최신 환율 목록을 반환한다")
		void shouldReturnLatestRatesForAllCurrencies() {
			// given
			LocalDateTime now = LocalDateTime.now();
			List<ExchangeRate> entities = List.of(
				ExchangeRate.builder()
					.currency(CurrencyCode.USD)
					.baseRate(new BigDecimal("1350.00"))
					.buyRate(new BigDecimal("1417.50"))
					.sellRate(new BigDecimal("1282.50"))
					.collectedAt(now)
					.build(),
				ExchangeRate.builder()
					.currency(CurrencyCode.JPY)
					.baseRate(new BigDecimal("900.00"))
					.buyRate(new BigDecimal("945.00"))
					.sellRate(new BigDecimal("855.00"))
					.collectedAt(now)
					.build()
			);
			given(exchangeRateRepository.findLatestRatesForAllCurrencies()).willReturn(entities);

			// when
			ExchangeRateListResponse response = exchangeRateService.getLatestRates();

			// then
			assertThat(response.exchangeRateList()).hasSize(2);
			assertThat(response.exchangeRateList().get(0).currencyCode()).isEqualTo(CurrencyCode.USD);
			assertThat(response.exchangeRateList().get(1).currencyCode()).isEqualTo(CurrencyCode.JPY);
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
			ExchangeRate entity = ExchangeRate.builder()
				.currency(CurrencyCode.USD)
				.baseRate(new BigDecimal("1350.00"))
				.buyRate(new BigDecimal("1417.50"))
				.sellRate(new BigDecimal("1282.50"))
				.collectedAt(now)
				.build();
			given(exchangeRateRepository.findTopByCurrencyOrderByCollectedAtDesc(CurrencyCode.USD))
				.willReturn(Optional.of(entity));

			// when
			ExchangeRateResponse response = exchangeRateService.getLatestRate(CurrencyCode.USD);

			// then
			assertThat(response.currencyCode()).isEqualTo(CurrencyCode.USD);
			assertThat(response.tradeStanRate()).isEqualByComparingTo(new BigDecimal("1350.00"));
			assertThat(response.buyRate()).isEqualByComparingTo(new BigDecimal("1417.50"));
			assertThat(response.sellRate()).isEqualByComparingTo(new BigDecimal("1282.50"));
			assertThat(response.dateTime()).isEqualTo(now);
		}

		@Test
		@DisplayName("환율 정보가 없으면 BusinessException이 발생한다")
		void shouldThrowExceptionWhenRateNotFound() {
			// given
			given(exchangeRateRepository.findTopByCurrencyOrderByCollectedAtDesc(CurrencyCode.CNY))
				.willReturn(Optional.empty());

			// when & then
			assertThatThrownBy(() -> exchangeRateService.getLatestRate(CurrencyCode.CNY))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(ErrorCode.EXCHANGE_RATE_NOT_FOUND);
		}
	}
}
