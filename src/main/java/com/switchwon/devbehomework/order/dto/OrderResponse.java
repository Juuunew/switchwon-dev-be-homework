package com.switchwon.devbehomework.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.switchwon.devbehomework.order.entity.ExchangeOrderEntity;
import com.switchwon.devbehomework.order.enums.OrderDirection;

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
	public static OrderResponse from(ExchangeOrderEntity order) {
		boolean isBuy = order.getDirection() == OrderDirection.BUY;
		if (isBuy) {
			return new OrderResponse(
				order.getId(),
				order.getKrwAmount(), "KRW",
				order.getForexAmount(), order.getCurrency().name(),
				order.getTradeRate(), order.getCreatedAt()
			);
		}
		return new OrderResponse(
			order.getId(),
			order.getForexAmount(), order.getCurrency().name(),
			order.getKrwAmount(), "KRW",
			order.getTradeRate(), order.getCreatedAt()
		);
	}
}
