package com.switchwon.devbehomework.exchangerate.provider;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.switchwon.devbehomework.currency.CurrencyCode;
import com.switchwon.devbehomework.currency.ForeignCurrency;

/**
 * 최후 방어 Provider. 외부 API 전부 장애 시 또는 로컬/테스트 환경에서 고정 환율을 반환한다.
 * unitRate는 1단위 외화 기준 (rateUnit 반영 전).
 */
@Component
public class MockExchangeRateProvider implements ExchangeRateProvider {

	@Override
	public ProviderRate fetchRate(ForeignCurrency from, CurrencyCode to) {
		BigDecimal unitRate = switch (from) {
			case USD -> new BigDecimal("1400");
			case JPY -> new BigDecimal("9.20");
			case CNY -> new BigDecimal("190");
			case EUR -> new BigDecimal("1490");
		};
		return new ProviderRate(from, to, unitRate);
	}

	@Override
	public boolean supports(ForeignCurrency from, CurrencyCode to) {
		return to == CurrencyCode.KRW;
	}

	@Override
	public String getName() {
		return "MOCK";
	}
}
