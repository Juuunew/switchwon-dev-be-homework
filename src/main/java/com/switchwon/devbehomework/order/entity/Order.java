package com.switchwon.devbehomework.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "orders")
@Entity
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, precision = 18, scale = 2)
	private BigDecimal fromAmount;

	@Column(nullable = false)
	private String fromCurrency;

	@Column(nullable = false, precision = 18, scale = 2)
	private BigDecimal toAmount;

	@Column(nullable = false)
	private String toCurrency;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal tradeRate;

	@Column(nullable = false)
	private LocalDateTime orderedAt;

	@Builder
	public Order(BigDecimal fromAmount, String fromCurrency, BigDecimal toAmount,
		String toCurrency, BigDecimal tradeRate, LocalDateTime orderedAt) {
		this.fromAmount = fromAmount;
		this.fromCurrency = fromCurrency;
		this.toAmount = toAmount;
		this.toCurrency = toCurrency;
		this.tradeRate = tradeRate;
		this.orderedAt = orderedAt;
	}

	public static Order of(BigDecimal fromAmount, String fromCurrency, BigDecimal toAmount,
		String toCurrency, BigDecimal tradeRate, LocalDateTime orderedAt) {
		return Order.builder()
			.fromAmount(fromAmount)
			.fromCurrency(fromCurrency)
			.toAmount(toAmount)
			.toCurrency(toCurrency)
			.tradeRate(tradeRate)
			.orderedAt(orderedAt)
			.build();
	}
}
