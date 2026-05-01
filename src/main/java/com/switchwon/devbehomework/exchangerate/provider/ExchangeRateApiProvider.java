package com.switchwon.devbehomework.exchangerate.provider;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.switchwon.devbehomework.common.enums.CurrencyCode;
import com.switchwon.devbehomework.common.enums.ErrorCode;
import com.switchwon.devbehomework.common.enums.ForeignCurrency;
import com.switchwon.devbehomework.common.exception.BusinessException;
import com.switchwon.devbehomework.exchangerate.dto.ExchangeRateResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Primary
@RequiredArgsConstructor
public class ExchangeRateApiProvider implements ExchangeRateProvider {

	private final RestTemplate restTemplate;

	@Value("${exchange-rate-api.request-url}")
	private String requestUrl;

	@Override
	public List<ExchangeRateResponse> fetchRates() {
		JsonNode response;
		try {
			response = restTemplate.getForObject(requestUrl, JsonNode.class);
		} catch (Exception ex) {
			log.error("Failed to call exchange rate API", ex);
			throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
		}

		if (response == null || !"success".equals(response.path("result").asText())) {
			throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR);
		}

		JsonNode conversionRates = response.path("conversion_rates");
		double krwPerUsd = conversionRates.path("KRW").asDouble();
		LocalDateTime now = LocalDateTime.now();

		return Arrays.stream(ForeignCurrency.values())
			.map(foreignCurrency -> {
				BigDecimal baseRate = calculateBaseRate(foreignCurrency, conversionRates, krwPerUsd);
				BigDecimal buyRate = baseRate.multiply(new BigDecimal("1.05")).setScale(2, RoundingMode.HALF_UP);
				BigDecimal sellRate = baseRate.multiply(new BigDecimal("0.95")).setScale(2, RoundingMode.HALF_UP);
				return ExchangeRateResponse.builder()
					.currencyCode(CurrencyCode.valueOf(foreignCurrency.name()))
					.tradeStanRate(baseRate)
					.buyRate(buyRate)
					.sellRate(sellRate)
					.dateTime(now)
					.build();
			})
			.toList();
	}

	private BigDecimal calculateBaseRate(ForeignCurrency foreignCurrency, JsonNode conversionRates, double krwPerUsd) {
		BigDecimal baseRate;

		if (foreignCurrency == ForeignCurrency.USD) {
			baseRate = BigDecimal.valueOf(krwPerUsd);
		} else {
			double foreignPerUsd = conversionRates.path(foreignCurrency.name()).asDouble();
			baseRate = BigDecimal.valueOf(krwPerUsd)
				.divide(BigDecimal.valueOf(foreignPerUsd), 10, RoundingMode.HALF_UP);
		}

		if (foreignCurrency.getRateUnit() > 1) {
			baseRate = baseRate.multiply(BigDecimal.valueOf(foreignCurrency.getRateUnit()));
		}

		double fluctuation = ThreadLocalRandom.current().nextDouble(-0.005, 0.005);
		baseRate = baseRate.multiply(BigDecimal.ONE.add(BigDecimal.valueOf(fluctuation)));

		return baseRate.setScale(2, RoundingMode.HALF_UP);
	}
}
