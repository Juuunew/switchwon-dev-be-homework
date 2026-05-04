package com.switchwon.devbehomework.exchangerate.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.switchwon.devbehomework.common.enums.ErrorCode;
import com.switchwon.devbehomework.common.exception.BusinessException;
import com.switchwon.devbehomework.currency.Currency;
import com.switchwon.devbehomework.currency.RatedCurrency;
import com.switchwon.devbehomework.exchangerate.dto.ExchangeRateListResponse;
import com.switchwon.devbehomework.exchangerate.dto.ExchangeRateResponse;
import com.switchwon.devbehomework.exchangerate.service.ExchangeRateService;

@WebMvcTest(ExchangeRateController.class)
@DisplayName("ExchangeRateController 테스트")
class ExchangeRateControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ExchangeRateService exchangeRateService;

	@Nested
	@DisplayName("GET /exchange-rate/latest")
	class GetLatestRates {

		@Test
		@DisplayName("전체 통화의 최신 환율 목록을 성공적으로 반환한다")
		void shouldReturnAllLatestRates() throws Exception {
			// given
			LocalDateTime now = LocalDateTime.now();
			List<ExchangeRateResponse> rates = List.of(
				ExchangeRateResponse.builder()
					.currencyCode(RatedCurrency.USD)
					.tradeStanRate(new BigDecimal("1350.00"))
					.buyRate(new BigDecimal("1417.50"))
					.sellRate(new BigDecimal("1282.50"))
					.dateTime(now)
					.build()
			);
			given(exchangeRateService.getLatestRates(Currency.KRW)).willReturn(ExchangeRateListResponse.from(rates));

			// when & then
			mockMvc.perform(get("/exchange-rate/latest"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("OK"))
				.andExpect(jsonPath("$.message").value("정상적으로 처리되었습니다."))
				.andExpect(jsonPath("$.returnObject.exchangeRateList").isArray())
				.andExpect(jsonPath("$.returnObject.exchangeRateList[0].currency").value("USD"))
				.andExpect(jsonPath("$.returnObject.exchangeRateList[0].tradeStanRate").value(1350.00))
				.andExpect(jsonPath("$.returnObject.exchangeRateList[0].buyRate").value(1417.50))
				.andExpect(jsonPath("$.returnObject.exchangeRateList[0].sellRate").value(1282.50));
		}
	}

	@Nested
	@DisplayName("GET /exchange-rate/latest/{currency}")
	class GetLatestRate {

		@Test
		@DisplayName("특정 통화의 최신 환율을 성공적으로 반환한다")
		void shouldReturnLatestRateForCurrency() throws Exception {
			// given
			LocalDateTime now = LocalDateTime.now();
			ExchangeRateResponse rate = ExchangeRateResponse.builder()
				.currencyCode(RatedCurrency.USD)
				.tradeStanRate(new BigDecimal("1350.00"))
				.buyRate(new BigDecimal("1417.50"))
				.sellRate(new BigDecimal("1282.50"))
				.dateTime(now)
				.build();
			given(exchangeRateService.getLatestRate(Currency.KRW, Currency.USD)).willReturn(rate);

			// when & then
			mockMvc.perform(get("/exchange-rate/latest/USD"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("OK"))
				.andExpect(jsonPath("$.returnObject.currency").value("USD"))
				.andExpect(jsonPath("$.returnObject.tradeStanRate").value(1350.00));
		}

		@Test
		@DisplayName("유효하지 않은 통화 코드로 조회하면 400을 반환한다")
		void shouldReturn400WhenInvalidCurrency() throws Exception {
			mockMvc.perform(get("/exchange-rate/latest/INVALID"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("400"));
		}

		@Test
		@DisplayName("환율 정보가 없는 통화를 조회하면 404를 반환한다")
		void shouldReturn404WhenRateNotFound() throws Exception {
			// given
			given(exchangeRateService.getLatestRate(Currency.KRW, Currency.CNY))
				.willThrow(new BusinessException(ErrorCode.EXCHANGE_RATE_NOT_FOUND));

			// when & then
			mockMvc.perform(get("/exchange-rate/latest/CNY"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("404"))
				.andExpect(jsonPath("$.message").value("환율 정보를 찾을 수 없습니다."));
		}
	}
}
