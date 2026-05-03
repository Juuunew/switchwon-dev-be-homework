package com.switchwon.devbehomework.exchangerate.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.switchwon.devbehomework.currency.CurrencyCode;
import com.switchwon.devbehomework.currency.ForeignCurrency;
import com.switchwon.devbehomework.exchangerate.entity.ExchangeRateEntity;
import com.switchwon.devbehomework.exchangerate.provider.ExchangeRateProvider;
import com.switchwon.devbehomework.exchangerate.provider.ProviderRate;
import com.switchwon.devbehomework.exchangerate.repository.ExchangeRateRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ProviderFallbackExchangeRateCollector 단위 테스트")
class ProviderFallbackExchangeRateCollectorTest {

	@Mock
	private ExchangeRateProvider primary;

	@Mock
	private ExchangeRateProvider fallback;

	@Mock
	private ExchangeRateRepository exchangeRateRepository;

	@Mock
	private Clock clock;

	private ProviderFallbackExchangeRateCollector collector;

	@BeforeEach
	void setUp() {
		given(clock.instant()).willReturn(Instant.now());
		given(clock.getZone()).willReturn(ZoneId.of("Asia/Seoul"));
		given(primary.getName()).willReturn("PRIMARY");
		given(fallback.getName()).willReturn("FALLBACK");
		given(exchangeRateRepository.findTopByFromCurrencyAndToCurrencyOrderByDateTimeDesc(any(), any()))
			.willReturn(Optional.empty());

		// USD만 테스트 대상, 나머지는 supports=false
		given(primary.supports(any(), any())).willReturn(false);
		given(fallback.supports(any(), any())).willReturn(false);
		given(primary.supports(ForeignCurrency.USD, CurrencyCode.KRW)).willReturn(true);
		given(fallback.supports(ForeignCurrency.USD, CurrencyCode.KRW)).willReturn(true);

		collector = new ProviderFallbackExchangeRateCollector(
			List.of(primary, fallback), exchangeRateRepository, clock
		);
	}

	@Nested
	@DisplayName("collectAll - 수집 및 스프레드 계산")
	class CollectAll {

		@Test
		@DisplayName("1순위 Provider 성공 시 스프레드 적용해 저장한다")
		void shouldSaveWithSpreadWhenPrimarySucceeds() {
			// given: USD unitRate=1350 → baseRate=1350, buy=1417.50, sell=1282.50
			given(primary.fetchRate(ForeignCurrency.USD, CurrencyCode.KRW))
				.willReturn(new ProviderRate(ForeignCurrency.USD, CurrencyCode.KRW, new BigDecimal("1350")));

			// when
			collector.collectAll();

			// then
			ArgumentCaptor<ExchangeRateEntity> captor = ArgumentCaptor.forClass(ExchangeRateEntity.class);
			verify(exchangeRateRepository).save(captor.capture());

			ExchangeRateEntity saved = captor.getValue();
			assertThat(saved.getBaseRate()).isEqualByComparingTo(new BigDecimal("1350.00"));
			assertThat(saved.getBuyRate()).isEqualByComparingTo(new BigDecimal("1417.50"));
			assertThat(saved.getSellRate()).isEqualByComparingTo(new BigDecimal("1282.50"));
			assertThat(saved.getProvider()).isEqualTo("PRIMARY");
		}

		@Test
		@DisplayName("JPY rateUnit=100 적용해 baseRate를 계산한다")
		void shouldApplyRateUnitForJpy() {
			// given: JPY unitRate=9.00 → baseRate=9.00*100=900, buy=945, sell=855
			given(primary.supports(ForeignCurrency.JPY, CurrencyCode.KRW)).willReturn(true);
			given(fallback.supports(ForeignCurrency.JPY, CurrencyCode.KRW)).willReturn(true);
			given(primary.fetchRate(ForeignCurrency.JPY, CurrencyCode.KRW))
				.willReturn(new ProviderRate(ForeignCurrency.JPY, CurrencyCode.KRW, new BigDecimal("9.00")));
			given(primary.fetchRate(ForeignCurrency.USD, CurrencyCode.KRW))
				.willReturn(new ProviderRate(ForeignCurrency.USD, CurrencyCode.KRW, new BigDecimal("1350")));

			// when
			collector.collectAll();

			// then
			ArgumentCaptor<ExchangeRateEntity> captor = ArgumentCaptor.forClass(ExchangeRateEntity.class);
			verify(exchangeRateRepository, org.mockito.Mockito.times(2)).save(captor.capture());

			ExchangeRateEntity jpyEntity = captor.getAllValues().stream()
				.filter(e -> e.getFromCurrency() == ForeignCurrency.JPY)
				.findFirst().orElseThrow();

			assertThat(jpyEntity.getBaseRate()).isEqualByComparingTo(new BigDecimal("900.00"));
			assertThat(jpyEntity.getBuyRate()).isEqualByComparingTo(new BigDecimal("945.00"));
			assertThat(jpyEntity.getSellRate()).isEqualByComparingTo(new BigDecimal("855.00"));
		}

		@Test
		@DisplayName("1순위 실패 시 2순위 Provider로 fallback한다")
		void shouldFallbackToSecondProviderWhenPrimaryFails() {
			// given
			given(primary.fetchRate(ForeignCurrency.USD, CurrencyCode.KRW))
				.willThrow(new RuntimeException("API 장애"));
			given(fallback.fetchRate(ForeignCurrency.USD, CurrencyCode.KRW))
				.willReturn(new ProviderRate(ForeignCurrency.USD, CurrencyCode.KRW, new BigDecimal("1350")));

			// when
			collector.collectAll();

			// then
			ArgumentCaptor<ExchangeRateEntity> captor = ArgumentCaptor.forClass(ExchangeRateEntity.class);
			verify(exchangeRateRepository).save(captor.capture());
			assertThat(captor.getValue().getProvider()).isEqualTo("FALLBACK");
		}

		@Test
		@DisplayName("모든 Provider 실패 시 저장하지 않는다")
		void shouldNotSaveWhenAllProvidersFail() {
			// given
			given(primary.fetchRate(ForeignCurrency.USD, CurrencyCode.KRW))
				.willThrow(new RuntimeException("1순위 실패"));
			given(fallback.fetchRate(ForeignCurrency.USD, CurrencyCode.KRW))
				.willThrow(new RuntimeException("2순위 실패"));

			// when
			collector.collectAll();

			// then
			verify(exchangeRateRepository, never()).save(any());
		}

		@Test
		@DisplayName("supports() false인 Provider는 건너뛴다")
		void shouldSkipProviderWhenNotSupported() {
			// given: primary.supports(USD, KRW) = false
			given(primary.supports(ForeignCurrency.USD, CurrencyCode.KRW)).willReturn(false);
			given(fallback.fetchRate(ForeignCurrency.USD, CurrencyCode.KRW))
				.willReturn(new ProviderRate(ForeignCurrency.USD, CurrencyCode.KRW, new BigDecimal("1350")));

			// when
			collector.collectAll();

			// then
			verify(primary, never()).fetchRate(ForeignCurrency.USD, CurrencyCode.KRW);
			verify(exchangeRateRepository).save(any());
		}
	}

	@Nested
	@DisplayName("collectAll - Sanity Check")
	class SanityCheck {

		@Test
		@DisplayName("변동폭 15% 이상이면 해당 통화 수집을 스킵한다")
		void shouldSkipWhenChangeExceedsSkipThreshold() {
			// given: 이전 1000, 신규 1200 → 20% 변동 → SKIP
			ExchangeRateEntity prevEntity = ExchangeRateEntity.builder()
				.fromCurrency(ForeignCurrency.USD)
				.toCurrency(CurrencyCode.KRW)
				.baseRate(new BigDecimal("1000.00"))
				.buyRate(new BigDecimal("1050.00"))
				.sellRate(new BigDecimal("950.00"))
				.provider("PRIMARY")
				.dateTime(java.time.LocalDateTime.now())
				.build();
			given(exchangeRateRepository.findTopByFromCurrencyAndToCurrencyOrderByDateTimeDesc(
				ForeignCurrency.USD, CurrencyCode.KRW)).willReturn(Optional.of(prevEntity));
			given(primary.fetchRate(ForeignCurrency.USD, CurrencyCode.KRW))
				.willReturn(new ProviderRate(ForeignCurrency.USD, CurrencyCode.KRW, new BigDecimal("1200")));

			// when
			collector.collectAll();

			// then
			verify(exchangeRateRepository, never()).save(any());
		}

		@Test
		@DisplayName("변동폭 5% 미만이면 정상 저장한다")
		void shouldSaveWhenChangeIsBelowWarnThreshold() {
			// given: 이전 1000, 신규 1030 → 3% 변동 → 정상 저장
			ExchangeRateEntity prevEntity = ExchangeRateEntity.builder()
				.fromCurrency(ForeignCurrency.USD)
				.toCurrency(CurrencyCode.KRW)
				.baseRate(new BigDecimal("1000.00"))
				.buyRate(new BigDecimal("1050.00"))
				.sellRate(new BigDecimal("950.00"))
				.provider("PRIMARY")
				.dateTime(java.time.LocalDateTime.now())
				.build();
			given(exchangeRateRepository.findTopByFromCurrencyAndToCurrencyOrderByDateTimeDesc(
				ForeignCurrency.USD, CurrencyCode.KRW)).willReturn(Optional.of(prevEntity));
			given(primary.fetchRate(ForeignCurrency.USD, CurrencyCode.KRW))
				.willReturn(new ProviderRate(ForeignCurrency.USD, CurrencyCode.KRW, new BigDecimal("1030")));

			// when
			collector.collectAll();

			// then
			verify(exchangeRateRepository).save(any());
		}
	}
}
