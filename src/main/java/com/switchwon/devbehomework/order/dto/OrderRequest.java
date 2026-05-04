package com.switchwon.devbehomework.order.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
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
	@Digits(
		integer = 17,
		fraction = 2,
		message = "거래 금액은 정수부 17자리, 소수부 2자리까지 입력 가능합니다."
	)
	private BigDecimal forexAmount;

	@NotBlank(message = "출발 통화는 필수입니다.")
	private String fromCurrency;

	@NotBlank(message = "도착 통화는 필수입니다.")
	private String toCurrency;
}
