package com.switchwon.devbehomework.scheduler;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.switchwon.devbehomework.common.enums.CurrencyCode;
import com.switchwon.devbehomework.common.enums.ErrorCode;
import com.switchwon.devbehomework.common.exception.BusinessException;
import com.switchwon.devbehomework.exchangerate.dto.ExchangeRateResponse;
import com.switchwon.devbehomework.exchangerate.provider.ExchangeRateProvider;
import com.switchwon.devbehomework.exchangerate.service.ExchangeRateService;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateScheduler 단위 테스트")
class ExchangeRateSchedulerTest {

	@InjectMocks
	private ExchangeRateScheduler exchangeRateScheduler;

	@Mock
	private ExchangeRateProvider exchangeRateProvider;

	@Mock
	private ExchangeRateService exchangeRateService;

	@Test
	@DisplayName("환율 수집 성공 시 provider에서 조회한 환율을 service에 저장한다")
	void shouldFetchAndSaveRates() {
		// given
		List<ExchangeRateResponse> rates = List.of(
			ExchangeRateResponse.builder()
				.currencyCode(CurrencyCode.USD)
				.tradeStanRate(new BigDecimal("1350.00"))
				.buyRate(new BigDecimal("1417.50"))
				.sellRate(new BigDecimal("1282.50"))
				.dateTime(LocalDateTime.now())
				.build()
		);
		given(exchangeRateProvider.fetchRates()).willReturn(rates);

		// when
		exchangeRateScheduler.collectExchangeRates();

		// then
		then(exchangeRateService).should().saveRates(rates);
	}

	@Test
	@DisplayName("환율 수집 중 예외 발생 시 예외를 전파하지 않고 로그만 남긴다")
	void shouldNotPropagateExceptionWhenFetchFails() {
		// given
		given(exchangeRateProvider.fetchRates())
			.willThrow(new BusinessException(ErrorCode.EXTERNAL_API_ERROR));

		// when
		exchangeRateScheduler.collectExchangeRates();

		// then
		then(exchangeRateService).should(never()).saveRates(anyList());
	}
}
