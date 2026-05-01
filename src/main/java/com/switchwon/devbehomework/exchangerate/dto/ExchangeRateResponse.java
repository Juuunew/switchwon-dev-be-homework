package com.switchwon.devbehomework.exchangerate.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.switchwon.devbehomework.common.enums.CurrencyCode;
import com.switchwon.devbehomework.exchangerate.entity.ExchangeRate;

import lombok.Builder;

@Builder
public record ExchangeRateResponse(
	@JsonProperty("currency") CurrencyCode currencyCode,
	BigDecimal tradeStanRate,
	BigDecimal buyRate,
	BigDecimal sellRate,
	LocalDateTime dateTime
) {

	public static ExchangeRateResponse from(ExchangeRate entity) {
		return ExchangeRateResponse.builder()
			.currencyCode(entity.getCurrency())
			.tradeStanRate(entity.getBaseRate())
			.buyRate(entity.getBuyRate())
			.sellRate(entity.getSellRate())
			.dateTime(entity.getCollectedAt())
			.build();
	}
}
