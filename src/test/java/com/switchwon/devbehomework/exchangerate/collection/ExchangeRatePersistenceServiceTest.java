package com.switchwon.devbehomework.exchangerate.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.switchwon.devbehomework.currency.CurrencyCode;
import com.switchwon.devbehomework.currency.ForeignCurrency;
import com.switchwon.devbehomework.exchangerate.entity.ExchangeRateEntity;
import com.switchwon.devbehomework.exchangerate.provider.ProviderRate;
import com.switchwon.devbehomework.exchangerate.repository.ExchangeRateRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRatePersistenceService 단위 테스트")
class ExchangeRatePersistenceServiceTest {

	@InjectMocks
	private ExchangeRatePersistenceService persistenceService;

	@Mock
	private ExchangeRateRepository exchangeRateRepository;

	@Nested
	@DisplayName("saveIfValid 메서드")
	class SaveIfValid {

		@Test
		@DisplayName("유효한 USD 환율이면 스프레드를 적용해 저장한다")
		void shouldSaveUsdRateWithSpread() {
			// given
			givenNoPreviousRate();
			ProviderRate providerRate = new ProviderRate(
				ForeignCurrency.USD, CurrencyCode.KRW, new BigDecimal("1350")
			);

			// when
			boolean result = persistenceService.saveIfValid(providerRate, "PRIMARY", LocalDateTime.now());

			// then
			assertThat(result).isTrue();

			ArgumentCaptor<ExchangeRateEntity> captor = ArgumentCaptor.forClass(ExchangeRateEntity.class);
			verify(exchangeRateRepository).save(captor.capture());

			ExchangeRateEntity saved = captor.getValue();
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
				ForeignCurrency.JPY, CurrencyCode.KRW, new BigDecimal("9.00")
			);

			// when
			boolean result = persistenceService.saveIfValid(providerRate, "PRIMARY", LocalDateTime.now());

			// then
			assertThat(result).isTrue();

			ArgumentCaptor<ExchangeRateEntity> captor = ArgumentCaptor.forClass(ExchangeRateEntity.class);
			verify(exchangeRateRepository).save(captor.capture());

			ExchangeRateEntity saved = captor.getValue();
			assertThat(saved.getBaseRate()).isEqualByComparingTo(new BigDecimal("900.00"));
			assertThat(saved.getBuyRate()).isEqualByComparingTo(new BigDecimal("945.00"));
			assertThat(saved.getSellRate()).isEqualByComparingTo(new BigDecimal("855.00"));
		}

		@Test
		@DisplayName("unitRate가 null이면 저장하지 않는다")
		void shouldNotSaveWhenUnitRateIsNull() {
			// given
			ProviderRate providerRate = new ProviderRate(ForeignCurrency.USD, CurrencyCode.KRW, null);

			// when
			boolean result = persistenceService.saveIfValid(providerRate, "PRIMARY", LocalDateTime.now());

			// then
			assertThat(result).isFalse();
			verify(exchangeRateRepository, never()).save(any());
		}

		@Test
		@DisplayName("unitRate가 0 이하이면 저장하지 않는다")
		void shouldNotSaveWhenUnitRateIsNotPositive() {
			// given
			ProviderRate providerRate = new ProviderRate(
				ForeignCurrency.USD, CurrencyCode.KRW, BigDecimal.ZERO
			);

			// when
			boolean result = persistenceService.saveIfValid(providerRate, "PRIMARY", LocalDateTime.now());

			// then
			assertThat(result).isFalse();
			verify(exchangeRateRepository, never()).save(any());
		}

		@Test
		@DisplayName("변동폭 15% 이상이면 저장하지 않는다")
		void shouldNotSaveWhenChangeExceedsSkipThreshold() {
			// given
			ExchangeRateEntity prevEntity = ExchangeRateEntity.builder()
				.fromCurrency(ForeignCurrency.USD)
				.toCurrency(CurrencyCode.KRW)
				.baseRate(new BigDecimal("1000.00"))
				.buyRate(new BigDecimal("1050.00"))
				.sellRate(new BigDecimal("950.00"))
				.provider("PRIMARY")
				.dateTime(LocalDateTime.now())
				.build();
			given(exchangeRateRepository.findTopByFromCurrencyAndToCurrencyOrderByDateTimeDesc(
				ForeignCurrency.USD, CurrencyCode.KRW)).willReturn(Optional.of(prevEntity));

			ProviderRate providerRate = new ProviderRate(
				ForeignCurrency.USD, CurrencyCode.KRW, new BigDecimal("1200")
			);

			// when
			boolean result = persistenceService.saveIfValid(providerRate, "PRIMARY", LocalDateTime.now());

			// then
			assertThat(result).isFalse();
			verify(exchangeRateRepository, never()).save(any());
		}

		@Test
		@DisplayName("변동폭 5% 미만이면 저장한다")
		void shouldSaveWhenChangeIsBelowWarnThreshold() {
			// given
			ExchangeRateEntity prevEntity = ExchangeRateEntity.builder()
				.fromCurrency(ForeignCurrency.USD)
				.toCurrency(CurrencyCode.KRW)
				.baseRate(new BigDecimal("1000.00"))
				.buyRate(new BigDecimal("1050.00"))
				.sellRate(new BigDecimal("950.00"))
				.provider("PRIMARY")
				.dateTime(LocalDateTime.now())
				.build();
			given(exchangeRateRepository.findTopByFromCurrencyAndToCurrencyOrderByDateTimeDesc(
				ForeignCurrency.USD, CurrencyCode.KRW)).willReturn(Optional.of(prevEntity));

			ProviderRate providerRate = new ProviderRate(
				ForeignCurrency.USD, CurrencyCode.KRW, new BigDecimal("1030")
			);

			// when
			boolean result = persistenceService.saveIfValid(providerRate, "PRIMARY", LocalDateTime.now());

			// then
			assertThat(result).isTrue();
			verify(exchangeRateRepository).save(any());
		}
	}

	private void givenNoPreviousRate() {
		given(exchangeRateRepository.findTopByFromCurrencyAndToCurrencyOrderByDateTimeDesc(any(), any()))
			.willReturn(Optional.empty());
	}
}
