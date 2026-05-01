package com.switchwon.devbehomework.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.switchwon.devbehomework.order.entity.Order;

public record OrderCreateResponse(
	BigDecimal fromAmount,
	String fromCurrency,
	BigDecimal toAmount,
	String toCurrency,
	BigDecimal tradeRate,
	LocalDateTime dateTime
) {

	public static OrderCreateResponse from(Order order) {
		return new OrderCreateResponse(
			order.getFromAmount(),
			order.getFromCurrency(),
			order.getToAmount(),
			order.getToCurrency(),
			order.getTradeRate(),
			order.getOrderedAt()
		);
	}
}
