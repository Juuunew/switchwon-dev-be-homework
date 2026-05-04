package com.switchwon.devbehomework.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import com.switchwon.devbehomework.common.enums.ErrorCode;
import com.switchwon.devbehomework.common.exception.BusinessException;
import com.switchwon.devbehomework.currency.ForeignCurrency;
import com.switchwon.devbehomework.exchangerate.dto.ExchangeRateResponse;
import com.switchwon.devbehomework.exchangerate.service.ExchangeRateService;
import com.switchwon.devbehomework.order.dto.OrderListResponse;
import com.switchwon.devbehomework.order.dto.OrderRequest;
import com.switchwon.devbehomework.order.dto.OrderResponse;
import com.switchwon.devbehomework.order.entity.ExchangeOrderEntity;
import com.switchwon.devbehomework.order.entity.ExchangeOrderRequestEntity;
import com.switchwon.devbehomework.order.repository.ExchangeOrderRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderService 단위 테스트")
class OrderServiceTest {

	@InjectMocks
	private OrderService orderService;

	@Mock
	private ExchangeOrderRepository orderRepository;

	@Mock
	private ExchangeRateService exchangeRateService;

	@Mock
	private OrderTransactionService orderTransactionService;

	@Mock
	private Clock clock;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(orderService, "rateFreshnessMinutes", 5);
		given(clock.instant()).willReturn(Instant.now());
		given(clock.getZone()).willReturn(ZoneId.of("Asia/Seoul"));
		given(orderTransactionService.saveRequest(any(ExchangeOrderRequestEntity.class)))
			.willAnswer(inv -> inv.getArgument(0));
		given(orderTransactionService.saveOrder(any(ExchangeOrderEntity.class), any(ExchangeOrderRequestEntity.class)))
			.willAnswer(inv -> inv.getArgument(0));
	}

	@Nested
	@DisplayName("createOrder 메서드")
	class CreateOrder {

		@Test
		@DisplayName("KRW → USD 매수 주문 시 buyRate 적용")
		void shouldApplyBuyRateWhenBuyingForeignCurrency() {
			// given
			OrderRequest request = new OrderRequest(new BigDecimal("100"), "KRW", "USD");
			ExchangeRateResponse rate = ExchangeRateResponse.builder()
				.currencyCode(ForeignCurrency.USD)
				.tradeStanRate(new BigDecimal("1350.00"))
				.buyRate(new BigDecimal("1417.50"))
				.sellRate(new BigDecimal("1282.50"))
				.dateTime(LocalDateTime.now())
				.build();
			given(exchangeRateService.getLatestRate(ForeignCurrency.USD)).willReturn(rate);

			// when
			OrderResponse response = orderService.createOrder(request);

			// then
			assertThat(response.fromCurrency()).isEqualTo("KRW");
			assertThat(response.toCurrency()).isEqualTo("USD");
			assertThat(response.tradeRate()).isEqualByComparingTo(new BigDecimal("1417.50"));
			assertThat(response.toAmount()).isEqualByComparingTo(new BigDecimal("100"));
			assertThat(response.fromAmount()).isEqualByComparingTo(new BigDecimal("141750"));
		}

		@Test
		@DisplayName("USD → KRW 매도 주문 시 sellRate 적용")
		void shouldApplySellRateWhenSellingForeignCurrency() {
			// given
			OrderRequest request = new OrderRequest(new BigDecimal("100"), "USD", "KRW");
			ExchangeRateResponse rate = ExchangeRateResponse.builder()
				.currencyCode(ForeignCurrency.USD)
				.tradeStanRate(new BigDecimal("1350.00"))
				.buyRate(new BigDecimal("1417.50"))
				.sellRate(new BigDecimal("1282.50"))
				.dateTime(LocalDateTime.now())
				.build();
			given(exchangeRateService.getLatestRate(ForeignCurrency.USD)).willReturn(rate);

			// when
			OrderResponse response = orderService.createOrder(request);

			// then
			assertThat(response.fromCurrency()).isEqualTo("USD");
			assertThat(response.toCurrency()).isEqualTo("KRW");
			assertThat(response.tradeRate()).isEqualByComparingTo(new BigDecimal("1282.50"));
			assertThat(response.toAmount()).isEqualByComparingTo(new BigDecimal("128250"));
		}

		@Test
		@DisplayName("KRW 금액 계산 시 소수점 이하를 버림 처리한다")
		void shouldFloorKrwAmount() {
			// given
			// 3 * 1417.50 = 4252.50 → floor → 4252
			OrderRequest request = new OrderRequest(new BigDecimal("3"), "KRW", "USD");
			ExchangeRateResponse rate = ExchangeRateResponse.builder()
				.currencyCode(ForeignCurrency.USD)
				.tradeStanRate(new BigDecimal("1350.00"))
				.buyRate(new BigDecimal("1417.50"))
				.sellRate(new BigDecimal("1282.50"))
				.dateTime(LocalDateTime.now())
				.build();
			given(exchangeRateService.getLatestRate(ForeignCurrency.USD)).willReturn(rate);

			// when
			OrderResponse response = orderService.createOrder(request);

			// then
			assertThat(response.fromAmount()).isEqualByComparingTo(new BigDecimal("4252"));
		}

		@Test
		@DisplayName("KRW → JPY 매수 주문 시 100엔 단위 환율 적용")
		void shouldApplyRateUnitWhenBuyingJpy() {
			// given
			// 200엔 * 945원/100엔 = 1890원
			OrderRequest request = new OrderRequest(new BigDecimal("200"), "KRW", "JPY");
			ExchangeRateResponse rate = ExchangeRateResponse.builder()
				.currencyCode(ForeignCurrency.JPY)
				.tradeStanRate(new BigDecimal("900.00"))
				.buyRate(new BigDecimal("945.00"))
				.sellRate(new BigDecimal("855.00"))
				.dateTime(LocalDateTime.now())
				.build();
			given(exchangeRateService.getLatestRate(ForeignCurrency.JPY)).willReturn(rate);

			// when
			OrderResponse response = orderService.createOrder(request);

			// then
			assertThat(response.fromAmount()).isEqualByComparingTo(new BigDecimal("1890"));
			assertThat(response.toAmount()).isEqualByComparingTo(new BigDecimal("200"));
		}

		@Test
		@DisplayName("동일 통화 주문 시 SAME_CURRENCY 예외 발생")
		void shouldThrowExceptionWhenSameCurrency() {
			// given
			OrderRequest request = new OrderRequest(new BigDecimal("100"), "USD", "USD");

			// when & then
			assertThatThrownBy(() -> orderService.createOrder(request))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(ErrorCode.SAME_CURRENCY);
		}

		@Test
		@DisplayName("KRW 없는 통화 쌍 주문 시 INVALID_CURRENCY_PAIR 예외 발생")
		void shouldThrowExceptionWhenNoKrwInPair() {
			// given
			OrderRequest request = new OrderRequest(new BigDecimal("100"), "USD", "EUR");

			// when & then
			assertThatThrownBy(() -> orderService.createOrder(request))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_CURRENCY_PAIR);
		}

		@Test
		@DisplayName("지원하지 않는 통화 입력 시 UNSUPPORTED_CURRENCY 예외 발생")
		void shouldThrowExceptionWhenUnsupportedCurrency() {
			// given
			OrderRequest request = new OrderRequest(new BigDecimal("100"), "KRW", "GBP");

			// when & then
			assertThatThrownBy(() -> orderService.createOrder(request))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(ErrorCode.UNSUPPORTED_CURRENCY);
		}

		@Test
		@DisplayName("환율 신선도 초과 시 RATE_STALE 예외 발생")
		void shouldThrowRateStaleWhenRateIsExpired() {
			// given: 10분 전 환율 (freshness=5분 초과)
			OrderRequest request = new OrderRequest(new BigDecimal("100"), "KRW", "USD");
			ExchangeRateResponse staleRate = ExchangeRateResponse.builder()
				.currencyCode(ForeignCurrency.USD)
				.tradeStanRate(new BigDecimal("1350.00"))
				.buyRate(new BigDecimal("1417.50"))
				.sellRate(new BigDecimal("1282.50"))
				.dateTime(LocalDateTime.now().minusMinutes(10))
				.build();
			given(exchangeRateService.getLatestRate(ForeignCurrency.USD)).willReturn(staleRate);

			// when & then
			assertThatThrownBy(() -> orderService.createOrder(request))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(ErrorCode.RATE_STALE);
		}
	}

	@Nested
	@DisplayName("createOrder - 주문 요청 감사 로그")
	class CreateOrderAudit {

		@Test
		@DisplayName("주문 실패 시 markFailed가 호출된다")
		void shouldCallMarkFailedWhenOrderFails() {
			// given: 10분 전 환율로 RATE_STALE 유발
			OrderRequest request = new OrderRequest(new BigDecimal("100"), "KRW", "USD");
			ExchangeRateResponse staleRate = ExchangeRateResponse.builder()
				.currencyCode(ForeignCurrency.USD)
				.tradeStanRate(new BigDecimal("1350.00"))
				.buyRate(new BigDecimal("1417.50"))
				.sellRate(new BigDecimal("1282.50"))
				.dateTime(LocalDateTime.now().minusMinutes(10))
				.build();
			given(exchangeRateService.getLatestRate(ForeignCurrency.USD)).willReturn(staleRate);

			// when & then
			assertThatThrownBy(() -> orderService.createOrder(request))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(ErrorCode.RATE_STALE);

			ArgumentCaptor<ErrorCode> errorCodeCaptor = ArgumentCaptor.forClass(ErrorCode.class);
			verify(orderTransactionService).markFailed(
				any(ExchangeOrderRequestEntity.class), errorCodeCaptor.capture(), any(LocalDateTime.class));
			assertThat(errorCodeCaptor.getValue()).isEqualTo(ErrorCode.RATE_STALE);
		}
	}

	@Nested
	@DisplayName("getOrders 메서드")
	class GetOrders {

		@Test
		@DisplayName("주문이 없으면 빈 페이지를 반환한다")
		void shouldReturnEmptyPageWhenNoOrders() {
			// given
			Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
			given(orderRepository.findAll(pageable))
				.willReturn(new PageImpl<>(List.of(), pageable, 0));

			// when
			OrderListResponse response = orderService.getOrders(pageable);

			// then
			assertThat(response.orderList()).isEmpty();
			assertThat(response.totalElements()).isZero();
			assertThat(response.totalPages()).isZero();
			assertThat(response.page()).isZero();
			assertThat(response.size()).isEqualTo(20);
		}
	}
}
