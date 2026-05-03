package com.switchwon.devbehomework.order.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.switchwon.devbehomework.common.enums.ErrorCode;
import com.switchwon.devbehomework.order.enums.OrderRequestStatus;

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
@Table(name = "exchange_order_request")
@Entity
public class ExchangeOrderRequestEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, precision = 19, scale = 2)
	private BigDecimal forexAmount;

	@Column(nullable = false, length = 10)
	private String fromCurrency;

	@Column(nullable = false, length = 10)
	private String toCurrency;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private OrderRequestStatus status;

	@Column(length = 50)
	private String failureCode;

	@Column(length = 255)
	private String failureMessage;

	@Column(nullable = false)
	private LocalDateTime requestedAt;

	private LocalDateTime completedAt;

	@Builder
	private ExchangeOrderRequestEntity(
		BigDecimal forexAmount, String fromCurrency, String toCurrency,
		OrderRequestStatus status, LocalDateTime requestedAt
	) {
		this.forexAmount = forexAmount;
		this.fromCurrency = fromCurrency;
		this.toCurrency = toCurrency;
		this.status = status;
		this.requestedAt = requestedAt;
	}

	public static ExchangeOrderRequestEntity received(
		BigDecimal forexAmount, String fromCurrency, String toCurrency, LocalDateTime requestedAt
	) {
		return ExchangeOrderRequestEntity.builder()
			.forexAmount(forexAmount)
			.fromCurrency(fromCurrency)
			.toCurrency(toCurrency)
			.status(OrderRequestStatus.RECEIVED)
			.requestedAt(requestedAt)
			.build();
	}

	public void markSucceeded(LocalDateTime completedAt) {
		this.status = OrderRequestStatus.SUCCESS;
		this.completedAt = completedAt;
	}

	public void markFailed(ErrorCode errorCode, LocalDateTime completedAt) {
		this.status = OrderRequestStatus.FAILED;
		this.failureCode = errorCode.name();
		this.failureMessage = errorCode.getMessage();
		this.completedAt = completedAt;
	}
}
