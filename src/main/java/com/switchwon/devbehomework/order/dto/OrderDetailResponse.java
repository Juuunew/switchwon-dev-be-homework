package com.switchwon.devbehomework.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.switchwon.devbehomework.order.entity.Order;

public record OrderDetailResponse(
	Long id,
	BigDecimal fromAmount,
	String fromCurrency,
	BigDecimal toAmount,
	String toCurrency,
	BigDecimal tradeRate,
	LocalDateTime dateTime
) {

	public static OrderDetailResponse from(Order order) {
		return new OrderDetailResponse(
			order.getId(),
			order.getFromAmount(),
			order.getFromCurrency(),
			order.getToAmount(),
			order.getToCurrency(),
			order.getTradeRate(),
			order.getOrderedAt()
		);
	}
}
