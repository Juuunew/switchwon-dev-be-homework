package com.switchwon.devbehomework.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

public record OrderResponse(
	@Schema(example = "1") Long id,
	@Schema(example = "283500") BigDecimal fromAmount,
	@Schema(example = "KRW") String fromCurrency,
	@Schema(example = "200") BigDecimal toAmount,
	@Schema(example = "USD") String toCurrency,
	@Schema(example = "1417.50") BigDecimal tradeRate,
	@Schema(example = "2026-05-04T14:00:00") LocalDateTime dateTime
) {
}
