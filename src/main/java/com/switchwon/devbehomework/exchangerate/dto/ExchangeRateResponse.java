package com.switchwon.devbehomework.exchangerate.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.switchwon.devbehomework.currency.ForeignCurrency;
import com.switchwon.devbehomework.exchangerate.entity.ExchangeRateEntity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record ExchangeRateResponse(
	@Schema(example = "USD") @JsonProperty("currency") ForeignCurrency currencyCode,
	@Schema(example = "1350.00") BigDecimal tradeStanRate,
	@Schema(example = "1417.50") BigDecimal buyRate,
	@Schema(example = "1282.50") BigDecimal sellRate,
	@Schema(example = "2026-05-04T14:00:00") LocalDateTime dateTime
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
