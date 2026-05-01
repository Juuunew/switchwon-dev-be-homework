package com.switchwon.devbehomework.exchangerate.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.switchwon.devbehomework.common.enums.CurrencyCode;

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
@Table(name = "exchange_rate")
@Entity
public class ExchangeRate {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CurrencyCode currency;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal baseRate;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal buyRate;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal sellRate;

	@Column(nullable = false)
	private LocalDateTime collectedAt;

	@Builder
	public ExchangeRate(CurrencyCode currency, BigDecimal baseRate,
		BigDecimal buyRate, BigDecimal sellRate, LocalDateTime collectedAt) {
		this.currency = currency;
		this.baseRate = baseRate;
		this.buyRate = buyRate;
		this.sellRate = sellRate;
		this.collectedAt = collectedAt;
	}

	public static ExchangeRate of(CurrencyCode currency, BigDecimal baseRate,
		BigDecimal buyRate, BigDecimal sellRate, LocalDateTime collectedAt) {
		return ExchangeRate.builder()
			.currency(currency)
			.baseRate(baseRate)
			.buyRate(buyRate)
			.sellRate(sellRate)
			.collectedAt(collectedAt)
			.build();
	}
}
