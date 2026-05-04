package com.switchwon.devbehomework.exchangerate.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.switchwon.devbehomework.currency.Currency;
import com.switchwon.devbehomework.currency.RatedCurrency;
import com.switchwon.devbehomework.exchangerate.entity.ExchangeRateHistoryEntity;
import com.switchwon.devbehomework.exchangerate.provider.ProviderRate;
import com.switchwon.devbehomework.exchangerate.repository.ExchangeRateRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRatePersistenceService 단위 테스트")
class ExchangeRatePersistenceServiceTest {

	@InjectMocks
	private ExchangeRatePersistenceService persistenceService;

	@Mock
	private ExchangeRateRepository exchangeRateRepository;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(persistenceService, "spreadBuy", new BigDecimal("1.05"));
		ReflectionTestUtils.setField(persistenceService, "spreadSell", new BigDecimal("0.95"));
		ReflectionTestUtils.setField(persistenceService, "warnThreshold", new BigDecimal("0.05"));
		ReflectionTestUtils.setField(persistenceService, "skipThreshold", new BigDecimal("0.15"));
	}

	@Nested
	@DisplayName("saveIfValid 메서드")
	class SaveIfValid {

		@Test
		@DisplayName("유효한 USD 환율이면 스프레드를 적용해 저장한다")
		void shouldSaveUsdRateWithSpread() {
			// given
			givenNoPreviousRate();
			ProviderRate providerRate = new ProviderRate(
				RatedCurrency.USD, Currency.KRW, new BigDecimal("1350")
			);

			// when
			Optional<SavedExchangeRate> result = persistenceService.saveIfValid(
				providerRate, "PRIMARY", LocalDateTime.now()
			);

			// then
			assertThat(result).isPresent();
			assertThat(result.get().currency()).isEqualTo(RatedCurrency.USD);

			ArgumentCaptor<ExchangeRateHistoryEntity> captor = ArgumentCaptor.forClass(ExchangeRateHistoryEntity.class);
			verify(exchangeRateRepository).save(captor.capture());

			ExchangeRateHistoryEntity saved = captor.getValue();
			assertThat(saved.getBaseRate()).isEqualByComparingTo(new BigDecimal("1350.00"));
			assertThat(saved.getBuyRate()).isEqualByComparingTo(new BigDecimal("1417.50"));
			assertThat(saved.getSellRate()).isEqualByComparingTo(new BigDecimal("1282.50"));
			assertThat(saved.getProvider()).isEqualTo("PRIMARY");
		}

		@Test
		@DisplayName("JPY 환율이면 rateUnit=100을 적용해 저장한다")
		void shouldApplyRateUnitForJpy() {
			// given
			givenNoPreviousRate();
			ProviderRate providerRate = new ProviderRate(
				RatedCurrency.JPY, Currency.KRW, new BigDecimal("9.00")
			);

			// when
			Optional<SavedExchangeRate> result = persistenceService.saveIfValid(
				providerRate, "PRIMARY", LocalDateTime.now()
			);

			// then
			assertThat(result).isPresent();
			assertThat(result.get().currency()).isEqualTo(RatedCurrency.JPY);

			ArgumentCaptor<ExchangeRateHistoryEntity> captor = ArgumentCaptor.forClass(ExchangeRateHistoryEntity.class);
			verify(exchangeRateRepository).save(captor.capture());

			ExchangeRateHistoryEntity saved = captor.getValue();
			assertThat(saved.getBaseRate()).isEqualByComparingTo(new BigDecimal("900.00"));
			assertThat(saved.getBuyRate()).isEqualByComparingTo(new BigDecimal("945.00"));
			assertThat(saved.getSellRate()).isEqualByComparingTo(new BigDecimal("855.00"));
		}

		@Test
		@DisplayName("unitRate가 null이면 저장하지 않는다")
		void shouldNotSaveWhenUnitRateIsNull() {
			// given
			ProviderRate providerRate = new ProviderRate(RatedCurrency.USD, Currency.KRW, null);

			// when
			Optional<SavedExchangeRate> result = persistenceService.saveIfValid(
				providerRate, "PRIMARY", LocalDateTime.now()
			);

			// then
			assertThat(result).isEmpty();
			verify(exchangeRateRepository, never()).save(any());
		}

		@Test
		@DisplayName("unitRate가 0 이하이면 저장하지 않는다")
		void shouldNotSaveWhenUnitRateIsNotPositive() {
			// given
			ProviderRate providerRate = new ProviderRate(
				RatedCurrency.USD, Currency.KRW, BigDecimal.ZERO
			);

			// when
			Optional<SavedExchangeRate> result = persistenceService.saveIfValid(
				providerRate, "PRIMARY", LocalDateTime.now()
			);

			// then
			assertThat(result).isEmpty();
			verify(exchangeRateRepository, never()).save(any());
		}

		@Test
		@DisplayName("변동폭 15% 이상이면 저장하지 않는다")
		void shouldNotSaveWhenChangeExceedsSkipThreshold() {
			// given
			ExchangeRateHistoryEntity prevEntity = ExchangeRateHistoryEntity.builder()
				.fromCurrency(RatedCurrency.USD)
				.toCurrency(Currency.KRW)
				.baseRate(new BigDecimal("1000.00"))
				.buyRate(new BigDecimal("1050.00"))
				.sellRate(new BigDecimal("950.00"))
				.provider("PRIMARY")
				.dateTime(LocalDateTime.now())
				.build();
			given(exchangeRateRepository.findTopByFromCurrencyAndToCurrencyOrderByDateTimeDesc(
				RatedCurrency.USD, Currency.KRW)).willReturn(Optional.of(prevEntity));

			ProviderRate providerRate = new ProviderRate(
				RatedCurrency.USD, Currency.KRW, new BigDecimal("1200")
			);

			// when
			Optional<SavedExchangeRate> result = persistenceService.saveIfValid(
				providerRate, "PRIMARY", LocalDateTime.now()
			);

			// then
			assertThat(result).isEmpty();
			verify(exchangeRateRepository, never()).save(any());
		}

		@Test
		@DisplayName("변동폭 5% 미만이면 저장한다")
		void shouldSaveWhenChangeIsBelowWarnThreshold() {
			// given
			ExchangeRateHistoryEntity prevEntity = ExchangeRateHistoryEntity.builder()
				.fromCurrency(RatedCurrency.USD)
				.toCurrency(Currency.KRW)
				.baseRate(new BigDecimal("1000.00"))
				.buyRate(new BigDecimal("1050.00"))
				.sellRate(new BigDecimal("950.00"))
				.provider("PRIMARY")
				.dateTime(LocalDateTime.now())
				.build();
			given(exchangeRateRepository.findTopByFromCurrencyAndToCurrencyOrderByDateTimeDesc(
				RatedCurrency.USD, Currency.KRW)).willReturn(Optional.of(prevEntity));
			given(exchangeRateRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

			ProviderRate providerRate = new ProviderRate(
				RatedCurrency.USD, Currency.KRW, new BigDecimal("1030")
			);

			// when
			Optional<SavedExchangeRate> result = persistenceService.saveIfValid(
				providerRate, "PRIMARY", LocalDateTime.now()
			);

			// then
			assertThat(result).isPresent();
			verify(exchangeRateRepository).save(any());
		}
	}

	private void givenNoPreviousRate() {
		given(exchangeRateRepository.findTopByFromCurrencyAndToCurrencyOrderByDateTimeDesc(any(), any()))
			.willReturn(Optional.empty());
		given(exchangeRateRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
	}
}
