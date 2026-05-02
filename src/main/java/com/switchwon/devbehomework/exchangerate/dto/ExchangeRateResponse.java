package com.switchwon.devbehomework.exchangerate.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.switchwon.devbehomework.currency.ForeignCurrency;
import com.switchwon.devbehomework.exchangerate.entity.ExchangeRateEntity;

import lombok.Builder;

@Builder
public record ExchangeRateResponse(
	@JsonProperty("currency") ForeignCurrency currencyCode,
	BigDecimal tradeStanRate,
	BigDecimal buyRate,
	BigDecimal sellRate,
	LocalDateTime dateTime
) {

	public static ExchangeRateResponse from(ExchangeRateEntity entity) {
		return ExchangeRateResponse.builder()
			.currencyCode(entity.getFromCurrency())
			.tradeStanRate(entity.getBaseRate())
			.buyRate(entity.getBuyRate())
			.sellRate(entity.getSellRate())
			.dateTime(entity.getDateTime())
			.build();
	}
}
