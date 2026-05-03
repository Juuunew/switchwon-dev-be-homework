package com.switchwon.devbehomework.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderResponse(
	Long id,
	BigDecimal fromAmount,
	String fromCurrency,
	BigDecimal toAmount,
	String toCurrency,
	BigDecimal tradeRate,
	LocalDateTime dateTime
) {
}
