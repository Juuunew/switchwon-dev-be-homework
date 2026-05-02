package com.switchwon.devbehomework.exchangerate.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

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
import com.switchwon.devbehomework.currency.CurrencyCode;
import com.switchwon.devbehomework.currency.ForeignCurrency;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateApiProvider 단위 테스트")
class ExchangeRateApiProviderTest {

	@InjectMocks
	private ExchangeRateApiProvider provider;

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private RestClient restClient;

	@Mock
	private Clock clock;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(provider, "requestUrl", "https://test.example.com/latest/USD");
	}

	@Nested
	@DisplayName("fetchRate 메서드")
	class FetchRate {

		@BeforeEach
		void setUpClock() {
			given(clock.instant()).willReturn(Instant.now());
			given(clock.getZone()).willReturn(ZoneId.of("Asia/Seoul"));
		}

		@Test
		@DisplayName("USD 환율 조회 시 KRW/USD 비율을 반환한다")
		void shouldReturnUsdRate() {
			// given
			ExchangeRateApiProvider.ConversionRates rates = new ExchangeRateApiProvider.ConversionRates(
				new BigDecimal("1350.0"), new BigDecimal("150.0"),
				new BigDecimal("7.24"), new BigDecimal("0.92")
			);
			ExchangeRateApiProvider.ExchangeRateApiResponse mockResponse =
				new ExchangeRateApiProvider.ExchangeRateApiResponse("success", rates);
			when(restClient.get().uri(anyString()).retrieve()
				.body(any(Class.class))).thenReturn(mockResponse);

			// when
			ProviderRate rate = provider.fetchRate(ForeignCurrency.USD, CurrencyCode.KRW);

			// then
			assertThat(rate.from()).isEqualTo(ForeignCurrency.USD);
			assertThat(rate.to()).isEqualTo(CurrencyCode.KRW);
			assertThat(rate.unitRate()).isEqualByComparingTo(new BigDecimal("1350.0"));
		}

		@Test
		@DisplayName("JPY 환율 조회 시 크로스레이트로 1엔당 원화를 계산한다")
		void shouldCalculateJpyCrossRate() {
			// given
			// KRW: 1350.0 per USD, JPY: 150.0 per USD → 1 JPY = 1350/150 = 9.00 KRW
			ExchangeRateApiProvider.ConversionRates rates = new ExchangeRateApiProvider.ConversionRates(
				new BigDecimal("1350.0"), new BigDecimal("150.0"),
				new BigDecimal("7.24"), new BigDecimal("0.92")
			);
			ExchangeRateApiProvider.ExchangeRateApiResponse mockResponse =
				new ExchangeRateApiProvider.ExchangeRateApiResponse("success", rates);
			when(restClient.get().uri(anyString()).retrieve()
				.body(any(Class.class))).thenReturn(mockResponse);

			// when
			ProviderRate rate = provider.fetchRate(ForeignCurrency.JPY, CurrencyCode.KRW);

			// then
			// 1350 / 150 = 9.00 (1엔당 KRW, rateUnit 반영 전)
			assertThat(rate.unitRate()).isEqualByComparingTo(new BigDecimal("9.0"));
		}

		@Test
		@DisplayName("외부 API 서버 오류 시 BusinessException이 발생한다")
		void shouldThrowExceptionWhenApiFails() {
			// given
			when(restClient.get().uri(anyString()).retrieve()
				.body(any(Class.class))).thenThrow(new RuntimeException("Connection refused"));

			// when & then
			assertThatThrownBy(() -> provider.fetchRate(ForeignCurrency.USD, CurrencyCode.KRW))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(ErrorCode.EXTERNAL_API_ERROR);
		}

		@Test
		@DisplayName("API 응답의 result가 success가 아니면 BusinessException이 발생한다")
		void shouldThrowExceptionWhenResultIsNotSuccess() {
			// given
			ExchangeRateApiProvider.ExchangeRateApiResponse errorResponse =
				new ExchangeRateApiProvider.ExchangeRateApiResponse("error", null);
			when(restClient.get().uri(anyString()).retrieve()
				.body(any(Class.class))).thenReturn(errorResponse);

			// when & then
			assertThatThrownBy(() -> provider.fetchRate(ForeignCurrency.USD, CurrencyCode.KRW))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(ErrorCode.EXTERNAL_API_ERROR);
		}
	}

	@Nested
	@DisplayName("supports 메서드")
	class Supports {

		@Test
		@DisplayName("USD → KRW는 지원한다")
		void shouldSupportUsdToKrw() {
			assertThat(provider.supports(ForeignCurrency.USD, CurrencyCode.KRW)).isTrue();
		}

		@Test
		@DisplayName("USD → USD는 지원하지 않는다")
		void shouldNotSupportUsdToUsd() {
			assertThat(provider.supports(ForeignCurrency.USD, CurrencyCode.USD)).isFalse();
		}
	}

	@Test
	@DisplayName("getName은 EXCHANGE_RATE_API를 반환한다")
	void shouldReturnProviderName() {
		assertThat(provider.getName()).isEqualTo("EXCHANGE_RATE_API");
	}
}
