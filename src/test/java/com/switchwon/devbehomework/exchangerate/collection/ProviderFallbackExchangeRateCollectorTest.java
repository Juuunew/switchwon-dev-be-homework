package com.switchwon.devbehomework.exchangerate.collection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.switchwon.devbehomework.currency.CurrencyCode;
import com.switchwon.devbehomework.currency.ForeignCurrency;
import com.switchwon.devbehomework.exchangerate.provider.ExchangeRateProvider;
import com.switchwon.devbehomework.exchangerate.provider.ProviderRate;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProviderFallbackExchangeRateCollector 단위 테스트")
class ProviderFallbackExchangeRateCollectorTest {

	@Mock
	private ExchangeRateProvider primary;

	@Mock
	private ExchangeRateProvider fallback;

	@Mock
	private ExchangeRatePersistenceService persistenceService;

	@Mock
	private Clock clock;

	private ProviderFallbackExchangeRateCollector collector;

	@BeforeEach
	void setUp() {
		given(clock.instant()).willReturn(Instant.now());
		given(clock.getZone()).willReturn(ZoneId.of("Asia/Seoul"));
		given(primary.getName()).willReturn("PRIMARY");
		given(fallback.getName()).willReturn("FALLBACK");

		given(primary.supports(any(), any())).willReturn(false);
		given(fallback.supports(any(), any())).willReturn(false);
		given(primary.supports(ForeignCurrency.USD, CurrencyCode.KRW)).willReturn(true);
		given(fallback.supports(ForeignCurrency.USD, CurrencyCode.KRW)).willReturn(true);

		given(persistenceService.saveIfValid(any(), any(), any())).willReturn(true);

		collector = new ProviderFallbackExchangeRateCollector(
			List.of(primary, fallback), persistenceService, clock
		);
	}

	@Nested
	@DisplayName("collectAll 메서드")
	class CollectAll {

		@Test
		@DisplayName("1순위 Provider 성공 시 수집한 환율 저장을 요청한다")
		void shouldSaveWhenPrimarySucceeds() {
			// given
			ProviderRate providerRate = new ProviderRate(
				ForeignCurrency.USD, CurrencyCode.KRW, new BigDecimal("1350")
			);
			given(primary.fetchRate(ForeignCurrency.USD, CurrencyCode.KRW)).willReturn(providerRate);

			// when
			collector.collectAll();

			// then
			verify(persistenceService).saveIfValid(eq(providerRate), eq("PRIMARY"), any(LocalDateTime.class));
			verify(fallback, never()).fetchRate(ForeignCurrency.USD, CurrencyCode.KRW);
		}

		@Test
		@DisplayName("1순위 실패 시 2순위 Provider로 fallback한다")
		void shouldFallbackToSecondProviderWhenPrimaryFails() {
			// given
			ProviderRate fallbackRate = new ProviderRate(
				ForeignCurrency.USD, CurrencyCode.KRW, new BigDecimal("1350")
			);
			given(primary.fetchRate(ForeignCurrency.USD, CurrencyCode.KRW))
				.willThrow(new RuntimeException("API 장애"));
			given(fallback.fetchRate(ForeignCurrency.USD, CurrencyCode.KRW)).willReturn(fallbackRate);

			// when
			collector.collectAll();

			// then
			verify(persistenceService).saveIfValid(eq(fallbackRate), eq("FALLBACK"), any(LocalDateTime.class));
		}

		@Test
		@DisplayName("저장 정책에서 거절되면 다음 Provider로 fallback한다")
		void shouldFallbackWhenPersistenceRejectsRate() {
			// given
			ProviderRate primaryRate = new ProviderRate(
				ForeignCurrency.USD, CurrencyCode.KRW, new BigDecimal("1200")
			);
			ProviderRate fallbackRate = new ProviderRate(
				ForeignCurrency.USD, CurrencyCode.KRW, new BigDecimal("1350")
			);
			given(primary.fetchRate(ForeignCurrency.USD, CurrencyCode.KRW)).willReturn(primaryRate);
			given(fallback.fetchRate(ForeignCurrency.USD, CurrencyCode.KRW)).willReturn(fallbackRate);
			given(persistenceService.saveIfValid(eq(primaryRate), eq("PRIMARY"), any()))
				.willReturn(false);
			given(persistenceService.saveIfValid(eq(fallbackRate), eq("FALLBACK"), any()))
				.willReturn(true);

			// when
			collector.collectAll();

			// then
			verify(persistenceService).saveIfValid(eq(primaryRate), eq("PRIMARY"), any(LocalDateTime.class));
			verify(persistenceService).saveIfValid(eq(fallbackRate), eq("FALLBACK"), any(LocalDateTime.class));
		}

		@Test
		@DisplayName("모든 Provider 실패 시 저장을 요청하지 않는다")
		void shouldNotSaveWhenAllProvidersFail() {
			// given
			given(primary.fetchRate(ForeignCurrency.USD, CurrencyCode.KRW))
				.willThrow(new RuntimeException("1순위 실패"));
			given(fallback.fetchRate(ForeignCurrency.USD, CurrencyCode.KRW))
				.willThrow(new RuntimeException("2순위 실패"));

			// when
			collector.collectAll();

			// then
			verify(persistenceService, never()).saveIfValid(any(), any(), any());
		}

		@Test
		@DisplayName("supports() false인 Provider는 건너뛴다")
		void shouldSkipProviderWhenNotSupported() {
			// given
			ProviderRate fallbackRate = new ProviderRate(
				ForeignCurrency.USD, CurrencyCode.KRW, new BigDecimal("1350")
			);
			given(primary.supports(ForeignCurrency.USD, CurrencyCode.KRW)).willReturn(false);
			given(fallback.fetchRate(ForeignCurrency.USD, CurrencyCode.KRW)).willReturn(fallbackRate);

			// when
			collector.collectAll();

			// then
			verify(primary, never()).fetchRate(ForeignCurrency.USD, CurrencyCode.KRW);
			verify(persistenceService).saveIfValid(eq(fallbackRate), eq("FALLBACK"), any(LocalDateTime.class));
		}
	}
}
