package com.switchwon.devbehomework.exchangerate.provider;

import java.math.BigDecimal;

import com.switchwon.devbehomework.currency.Currency;
import com.switchwon.devbehomework.currency.RatedCurrency;

/**
 * Provider가 반환하는 raw 환율 데이터.
 * unitRate = 1단위 외화 기준 KRW (spread 미적용, rateUnit 미반영)
 * 예) JPY: 1엔 = 9.32원 → unitRate=9.32 (100엔 단위 변환은 Collector에서)
 */
public record ProviderRate(
	RatedCurrency from,
	Currency to,
	BigDecimal unitRate
) { }
