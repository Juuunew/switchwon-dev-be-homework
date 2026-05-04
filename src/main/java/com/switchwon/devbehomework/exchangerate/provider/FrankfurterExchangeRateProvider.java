package com.switchwon.devbehomework.exchangerate.provider;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.switchwon.devbehomework.common.enums.ErrorCode;
import com.switchwon.devbehomework.common.exception.BusinessException;
import com.switchwon.devbehomework.currency.CurrencyCode;
import com.switchwon.devbehomework.currency.ForeignCurrency;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Frankfurter API 기반 2순위 Fallback Provider.
 * USD 기준 환율에서 크로스레이트를 계산한다: unitRate = rates.krw / rates.foreignPerUsd
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FrankfurterExchangeRateProvider implements ExchangeRateProvider {

	private final RestClient restClient;

	@Value("${exchange-rate.providers.frankfurter.request-url}")
	private String frankfurterUrl;

	record FrankfurterResponse(
		String base,
		Rates rates
	) { }

	record Rates(
		@JsonProperty("KRW") BigDecimal krw,
		@JsonProperty("JPY") BigDecimal jpy,
		@JsonProperty("CNY") BigDecimal cny,
		@JsonProperty("EUR") BigDecimal eur
	) {
		BigDecimal ratePerUsd(ForeignCurrency currency) {
			return switch (currency) {
				case USD -> BigDecimal.ONE;
				case JPY -> jpy;
				case CNY -> cny;
				case EUR -> eur;
			};
		}
	}

	@Override
	public ProviderRate fetchRate(ForeignCurrency from, CurrencyCode to) {
		FrankfurterResponse response;
		try {
			response = restClient.get().uri(frankfurterUrl).retrieve()
				.body(FrankfurterResponse.class);
		} catch (Exception ex) {
			log.error("Frankfurter API 호출 실패: {}", ex.getMessage());
			throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
		}

		if (response == null || response.rates() == null) {
			log.error("Frankfurter API 응답 형식 오류");
			throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
		}

		Rates rates = response.rates();
		BigDecimal krwPerUsd = requirePositiveRate("KRW", rates.krw());
		BigDecimal foreignPerUsd = requirePositiveRate(from.name(), rates.ratePerUsd(from));
		BigDecimal unitRate = krwPerUsd.divide(foreignPerUsd, 10, RoundingMode.HALF_UP);
		return new ProviderRate(from, to, unitRate);
	}

	@Override
	public boolean supports(ForeignCurrency from, CurrencyCode to) {
		return to == CurrencyCode.KRW;
	}

	@Override
	public String getName() {
		return "FRANKFURTER";
	}

	private BigDecimal requirePositiveRate(String rateName, BigDecimal rate) {
		if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
			log.error("Frankfurter API 응답 환율 무효: {}={}", rateName, rate);
			throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
		}
		return rate;
	}
}
