package com.switchwon.devbehomework.exchangerate.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.switchwon.devbehomework.currency.CurrencyCode;
import com.switchwon.devbehomework.currency.ForeignCurrency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "exchange_rate", indexes = {
	@Index(name = "idx_er_from_to_datetime", columnList = "fromCurrency, toCurrency, dateTime")
})
@Entity
public class ExchangeRateEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private ForeignCurrency fromCurrency;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private CurrencyCode toCurrency;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal baseRate;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal buyRate;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal sellRate;

	@Column(nullable = false, length = 50)
	private String provider;

	@Column(nullable = false)
	private LocalDateTime dateTime;

	@Builder
	public ExchangeRateEntity(ForeignCurrency fromCurrency, CurrencyCode toCurrency,
		BigDecimal baseRate, BigDecimal buyRate, BigDecimal sellRate,
		String provider, LocalDateTime dateTime) {
		this.fromCurrency = fromCurrency;
		this.toCurrency = toCurrency;
		this.baseRate = baseRate;
		this.buyRate = buyRate;
		this.sellRate = sellRate;
		this.provider = provider;
		this.dateTime = dateTime;
	}
}
