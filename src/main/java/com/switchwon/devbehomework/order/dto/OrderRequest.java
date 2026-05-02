package com.switchwon.devbehomework.order.dto;

import java.math.BigDecimal;

import com.switchwon.devbehomework.currency.CurrencyCode;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

	@NotNull(message = "거래 금액은 필수입니다.")
	@Positive(message = "거래 금액은 0보다 커야 합니다.")
	private BigDecimal forexAmount;

	@NotNull(message = "출발 통화는 필수입니다.")
	private CurrencyCode fromCurrency;

	@NotNull(message = "도착 통화는 필수입니다.")
	private CurrencyCode toCurrency;
}
