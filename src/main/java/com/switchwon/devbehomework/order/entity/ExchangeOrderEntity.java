package com.switchwon.devbehomework.order.entity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import com.switchwon.devbehomework.currency.RatedCurrency;
import com.switchwon.devbehomework.exchangerate.dto.ExchangeRateResponse;
import com.switchwon.devbehomework.order.enums.OrderDirection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "exchange_order")
@Entity
public class ExchangeOrderEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private Long requestId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 4)
	private OrderDirection direction;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 3)
	private RatedCurrency currency;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal forexAmount;

	@Column(nullable = false, precision = 19, scale = 0)
	private BigDecimal krwAmount;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal tradeRate;

	@Column(nullable = false)
	private LocalDateTime rateDateTime;

	@Column(nullable = false)
	private LocalDateTime createdAt;

	@Builder
	private ExchangeOrderEntity(
		Long requestId, OrderDirection direction, RatedCurrency currency,
		BigDecimal forexAmount, BigDecimal krwAmount, BigDecimal tradeRate,
		LocalDateTime rateDateTime, LocalDateTime createdAt
	) {
		this.requestId = requestId;
		this.direction = direction;
		this.currency = currency;
		this.forexAmount = forexAmount;
		this.krwAmount = krwAmount;
		this.tradeRate = tradeRate;
		this.rateDateTime = rateDateTime;
		this.createdAt = createdAt;
	}

	public static ExchangeOrderEntity create(
		Long requestId, OrderDirection direction, RatedCurrency currency,
		BigDecimal forexAmount, ExchangeRateResponse rate, LocalDateTime now
	) {
		BigDecimal tradeRate = direction == OrderDirection.BUY ? rate.buyRate() : rate.sellRate();
		BigDecimal krwAmount = forexAmount.multiply(tradeRate)
			.divide(BigDecimal.valueOf(currency.getRateUnit()), 0, RoundingMode.FLOOR);

		return ExchangeOrderEntity.builder()
			.requestId(requestId)
			.direction(direction)
			.currency(currency)
			.forexAmount(forexAmount)
			.krwAmount(krwAmount)
			.tradeRate(tradeRate)
			.rateDateTime(rate.dateTime())
			.createdAt(now)
			.build();
	}
}
