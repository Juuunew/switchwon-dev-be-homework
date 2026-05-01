package com.switchwon.devbehomework.exchangerate.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.switchwon.devbehomework.common.enums.CurrencyCode;
import com.switchwon.devbehomework.common.enums.ErrorCode;
import com.switchwon.devbehomework.common.exception.BusinessException;
import com.switchwon.devbehomework.exchangerate.dto.ExchangeRateResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = "scheduler.exchange-rate.initial-delay=999999999")
@DisplayName("ExchangeRateApiProvider 테스트")
class ExchangeRateApiProviderTest {

	@Autowired
	private ExchangeRateApiProvider provider;

	@Autowired
	private RestTemplate restTemplate;

	@Value("${exchange-rate-api.request-url}")
	private String requestUrl;

	private MockRestServiceServer mockServer;

	@BeforeEach
	void setUp() {
		mockServer = MockRestServiceServer.bindTo(restTemplate).build();
	}

	@Nested
	@DisplayName("fetchRates 메서드")
	class FetchRates {

		@Test
		@DisplayName("외부 API 호출 성공 시 4개 통화의 환율을 반환한다")
		void shouldReturnFourCurrencyRates() {
			// given
			String mockResponse = """
				{
					"result": "success",
					"conversion_rates": {
						"KRW": 1350.0,
						"JPY": 150.0,
						"CNY": 7.24,
						"EUR": 0.92
					}
				}
				""";
			mockServer.expect(requestTo(requestUrl))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

			// when
			List<ExchangeRateResponse> rates = provider.fetchRates();

			// then
			mockServer.verify();
			assertThat(rates).hasSize(4);
		}

		@Test
		@DisplayName("매입율은 기준율보다 높고 매도율은 기준율보다 낮다")
		void shouldApplySpreadCorrectly() {
			// given
			String mockResponse = """
				{
					"result": "success",
					"conversion_rates": {
						"KRW": 1350.0,
						"JPY": 150.0,
						"CNY": 7.24,
						"EUR": 0.92
					}
				}
				""";
			mockServer.expect(requestTo(requestUrl))
				.andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

			// when
			List<ExchangeRateResponse> rates = provider.fetchRates();

			// then
			rates.forEach(rate -> {
				assertThat(rate.buyRate()).isGreaterThan(rate.tradeStanRate());
				assertThat(rate.sellRate()).isLessThan(rate.tradeStanRate());
			});
		}

		@Test
		@DisplayName("JPY 환율은 100엔 단위로 환산된다")
		void shouldCalculateJpyRateAs100YenUnit() {
			// given
			// KRW: 1350.0 per USD, JPY: 150.0 per USD
			// JPY base rate = (1350.0 / 150.0) * 100 = 900.00 (±0.5% 변동)
			String mockResponse = """
				{
					"result": "success",
					"conversion_rates": {
						"KRW": 1350.0,
						"JPY": 150.0,
						"CNY": 7.24,
						"EUR": 0.92
					}
				}
				""";
			mockServer.expect(requestTo(requestUrl))
				.andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

			// when
			List<ExchangeRateResponse> rates = provider.fetchRates();

			// then
			ExchangeRateResponse jpyRate = rates.stream()
				.filter(rate -> rate.currencyCode() == CurrencyCode.JPY)
				.findFirst()
				.orElseThrow();
			assertThat(jpyRate.tradeStanRate())
				.isBetween(new BigDecimal("895.50"), new BigDecimal("904.50"));
		}

		@Test
		@DisplayName("외부 API 서버 오류 시 BusinessException이 발생한다")
		void shouldThrowExceptionWhenApiFails() {
			// given
			mockServer.expect(requestTo(requestUrl))
				.andRespond(withServerError());

			// when & then
			assertThatThrownBy(() -> provider.fetchRates())
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(ErrorCode.EXTERNAL_API_ERROR);
		}

		@Test
		@DisplayName("API 응답의 result가 success가 아니면 BusinessException이 발생한다")
		void shouldThrowExceptionWhenResultIsNotSuccess() {
			// given
			String mockResponse = """
				{
					"result": "error",
					"error-type": "invalid-key"
				}
				""";
			mockServer.expect(requestTo(requestUrl))
				.andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

			// when & then
			assertThatThrownBy(() -> provider.fetchRates())
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(ErrorCode.EXTERNAL_API_ERROR);
		}
	}
}
