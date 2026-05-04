package com.switchwon.devbehomework.exchangerate.provider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;

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
 * ExchangeRate API v6 기반 환율 Provider.
 * 단일 USD-base 응답을 캐시해 수집 사이클 내 중복 API 호출을 방지한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateApiProvider implements ExchangeRateProvider {

	private final RestClient restClient;
	private final Clock clock;

	@Value("${exchange-rate.providers.exchange-rate-api.request-url}")
	private String requestUrl;

	private volatile ExchangeRateApiResponse rateCache;
	private volatile LocalDateTime cacheTime;

	record ExchangeRateApiResponse(
		String result,
		@JsonProperty("conversion_rates") ConversionRates conversionRates
	) { }

	record ConversionRates(
		@JsonProperty("KRW") BigDecimal krw,
		@JsonProperty("JPY") BigDecimal jpy,
		@JsonProperty("CNY") BigDecimal cny,
		@JsonProperty("EUR") BigDecimal eur
	) {
		/**
		 * 해당 외화의 USD 대비 환율 (= foreignPerUsd).
		 * USD 자신은 1로 처리해 크로스레이트 계산을 통일한다.
		 */
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
		ConversionRates rates = getApiResponse().conversionRates();
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
		return "EXCHANGE_RATE_API";
	}

	private ExchangeRateApiResponse getApiResponse() {
		LocalDateTime now = LocalDateTime.now(clock);
		if (rateCache != null && cacheTime != null
			&& cacheTime.isAfter(now.minusSeconds(30))) {
			return rateCache;
		}

		ExchangeRateApiResponse response;
		try {
			response = restClient.get().uri(requestUrl).retrieve()
				.body(ExchangeRateApiResponse.class);
		} catch (Exception ex) {
			log.error("ExchangeRateAPI 호출 실패: {}", ex.getMessage());
			throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
		}

		if (response == null || !"success".equals(response.result())
			|| response.conversionRates() == null) {
			log.error("ExchangeRateAPI 응답 오류: result={}", response != null ? response.result() : "null");
			throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
		}

		rateCache = response;
		cacheTime = now;
		return response;
	}

	private BigDecimal requirePositiveRate(String rateName, BigDecimal rate) {
		if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) {
			log.error("ExchangeRateAPI 응답 환율 무효: {}={}", rateName, rate);
			throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
		}
		return rate;
	}
}
