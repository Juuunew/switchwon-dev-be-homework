package com.switchwon.devbehomework.exchangerate.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import com.switchwon.devbehomework.common.enums.ErrorCode;
import com.switchwon.devbehomework.common.exception.BusinessException;
import com.switchwon.devbehomework.currency.Currency;
import com.switchwon.devbehomework.currency.RatedCurrency;

@ExtendWith(MockitoExtension.class)
@DisplayName("FrankfurterExchangeRateProvider 단위 테스트")
class FrankfurterExchangeRateProviderTest {

	@InjectMocks
	private FrankfurterExchangeRateProvider provider;

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private RestClient restClient;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(provider, "frankfurterUrl", "https://test.example.com/latest");
	}

	@Nested
	@DisplayName("fetchRate 메서드")
	class FetchRate {

		@Test
		@DisplayName("JPY 환율 조회 시 크로스레이트로 1엔당 원화를 계산한다")
		void shouldCalculateJpyCrossRate() {
			// given
			FrankfurterExchangeRateProvider.Rates rates = new FrankfurterExchangeRateProvider.Rates(
				new BigDecimal("1350.0"), new BigDecimal("150.0"),
				new BigDecimal("7.24"), new BigDecimal("0.92")
			);
			FrankfurterExchangeRateProvider.FrankfurterResponse mockResponse =
				new FrankfurterExchangeRateProvider.FrankfurterResponse("USD", rates);
			when(restClient.get().uri(anyString()).retrieve()
				.body(any(Class.class))).thenReturn(mockResponse);

			// when
			ProviderRate rate = provider.fetchRate(RatedCurrency.JPY, Currency.KRW);

			// then
			assertThat(rate.unitRate()).isEqualByComparingTo(new BigDecimal("9.0"));
		}

		@Test
		@DisplayName("API 응답에 rates가 없으면 BusinessException이 발생한다")
		void shouldThrowExceptionWhenRatesIsMissing() {
			// given
			FrankfurterExchangeRateProvider.FrankfurterResponse invalidResponse =
				new FrankfurterExchangeRateProvider.FrankfurterResponse("USD", null);
			when(restClient.get().uri(anyString()).retrieve()
				.body(any(Class.class))).thenReturn(invalidResponse);

			// when & then
			assertThatThrownBy(() -> provider.fetchRate(RatedCurrency.USD, Currency.KRW))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(ErrorCode.EXTERNAL_API_ERROR);
		}

		@Test
		@DisplayName("API 응답에 대상 통화 환율이 없으면 BusinessException이 발생한다")
		void shouldThrowExceptionWhenTargetCurrencyRateIsMissing() {
			// given
			FrankfurterExchangeRateProvider.Rates rates = new FrankfurterExchangeRateProvider.Rates(
				new BigDecimal("1350.0"), null,
				new BigDecimal("7.24"), new BigDecimal("0.92")
			);
			FrankfurterExchangeRateProvider.FrankfurterResponse invalidResponse =
				new FrankfurterExchangeRateProvider.FrankfurterResponse("USD", rates);
			when(restClient.get().uri(anyString()).retrieve()
				.body(any(Class.class))).thenReturn(invalidResponse);

			// when & then
			assertThatThrownBy(() -> provider.fetchRate(RatedCurrency.JPY, Currency.KRW))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(ErrorCode.EXTERNAL_API_ERROR);
		}

		@Test
		@DisplayName("API 응답의 환율이 0이면 BusinessException이 발생한다")
		void shouldThrowExceptionWhenRateIsZero() {
			// given
			FrankfurterExchangeRateProvider.Rates rates = new FrankfurterExchangeRateProvider.Rates(
				BigDecimal.ZERO, new BigDecimal("150.0"),
				new BigDecimal("7.24"), new BigDecimal("0.92")
			);
			FrankfurterExchangeRateProvider.FrankfurterResponse invalidResponse =
				new FrankfurterExchangeRateProvider.FrankfurterResponse("USD", rates);
			when(restClient.get().uri(anyString()).retrieve()
				.body(any(Class.class))).thenReturn(invalidResponse);

			// when & then
			assertThatThrownBy(() -> provider.fetchRate(RatedCurrency.USD, Currency.KRW))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(ErrorCode.EXTERNAL_API_ERROR);
		}
	}
}
