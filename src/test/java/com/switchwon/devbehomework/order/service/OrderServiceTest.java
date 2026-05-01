package com.switchwon.devbehomework.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.switchwon.devbehomework.common.enums.CurrencyCode;
import com.switchwon.devbehomework.common.enums.ErrorCode;
import com.switchwon.devbehomework.common.exception.BusinessException;
import com.switchwon.devbehomework.exchangerate.dto.ExchangeRateResponse;
import com.switchwon.devbehomework.exchangerate.service.ExchangeRateService;
import com.switchwon.devbehomework.order.dto.OrderCreateResponse;
import com.switchwon.devbehomework.order.dto.OrderListResponse;
import com.switchwon.devbehomework.order.dto.OrderRequest;
import com.switchwon.devbehomework.order.entity.Order;
import com.switchwon.devbehomework.order.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderService 단위 테스트")
class OrderServiceTest {

	@InjectMocks
	private OrderService orderService;

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private ExchangeRateService exchangeRateService;

	@Nested
	@DisplayName("createOrder 메서드")
	class CreateOrder {

		@Test
		@DisplayName("KRW에서 USD로 매수 주문 시 buyRate를 적용하고 KRW 금액을 버림 처리한다")
		void shouldApplyBuyRateWhenBuyingForeignCurrency() {
			// given
			OrderRequest request = new OrderRequest(
				new BigDecimal("100"),
				CurrencyCode.KRW,
				CurrencyCode.USD
			);
			ExchangeRateResponse rate = ExchangeRateResponse.builder()
				.currencyCode(CurrencyCode.USD)
				.tradeStanRate(new BigDecimal("1350.00"))
				.buyRate(new BigDecimal("1417.50"))
				.sellRate(new BigDecimal("1282.50"))
				.dateTime(LocalDateTime.now())
				.build();
			given(exchangeRateService.getLatestRate(CurrencyCode.USD)).willReturn(rate);
			given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

			// when
			OrderCreateResponse response = orderService.createOrder(request);

			// then
			assertThat(response.fromCurrency()).isEqualTo("KRW");
			assertThat(response.toCurrency()).isEqualTo("USD");
			assertThat(response.tradeRate()).isEqualByComparingTo(new BigDecimal("1417.50"));
			assertThat(response.toAmount()).isEqualByComparingTo(new BigDecimal("100"));
			assertThat(response.fromAmount()).isEqualByComparingTo(new BigDecimal("141750"));
			verify(orderRepository).save(any(Order.class));
		}

		@Test
		@DisplayName("USD에서 KRW로 매도 주문 시 sellRate를 적용하고 KRW 금액을 버림 처리한다")
		void shouldApplySellRateWhenSellingForeignCurrency() {
			// given
			OrderRequest request = new OrderRequest(
				new BigDecimal("100"),
				CurrencyCode.USD,
				CurrencyCode.KRW
			);
			ExchangeRateResponse rate = ExchangeRateResponse.builder()
				.currencyCode(CurrencyCode.USD)
				.tradeStanRate(new BigDecimal("1350.00"))
				.buyRate(new BigDecimal("1417.50"))
				.sellRate(new BigDecimal("1282.50"))
				.dateTime(LocalDateTime.now())
				.build();
			given(exchangeRateService.getLatestRate(CurrencyCode.USD)).willReturn(rate);
			given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

			// when
			OrderCreateResponse response = orderService.createOrder(request);

			// then
			assertThat(response.fromCurrency()).isEqualTo("USD");
			assertThat(response.toCurrency()).isEqualTo("KRW");
			assertThat(response.tradeRate()).isEqualByComparingTo(new BigDecimal("1282.50"));
			assertThat(response.fromAmount()).isEqualByComparingTo(new BigDecimal("100"));
			assertThat(response.toAmount()).isEqualByComparingTo(new BigDecimal("128250"));
			verify(orderRepository).save(any(Order.class));
		}

		@Test
		@DisplayName("KRW 금액 계산 시 소수점 이하를 버림 처리한다")
		void shouldFloorKrwAmount() {
			// given
			OrderRequest request = new OrderRequest(
				new BigDecimal("3"),
				CurrencyCode.KRW,
				CurrencyCode.USD
			);
			ExchangeRateResponse rate = ExchangeRateResponse.builder()
				.currencyCode(CurrencyCode.USD)
				.tradeStanRate(new BigDecimal("1350.00"))
				.buyRate(new BigDecimal("1417.50"))
				.sellRate(new BigDecimal("1282.50"))
				.dateTime(LocalDateTime.now())
				.build();
			given(exchangeRateService.getLatestRate(CurrencyCode.USD)).willReturn(rate);
			given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

			// when
			OrderCreateResponse response = orderService.createOrder(request);

			// then
			// 3 * 1417.50 = 4252.50 -> floor -> 4252
			assertThat(response.fromAmount()).isEqualByComparingTo(new BigDecimal("4252"));
		}

		@Test
		@DisplayName("KRW에서 JPY로 매수 주문 시 100엔 단위 환율을 적용한다")
		void shouldApplyRateUnitWhenBuyingJpy() {
			// given
			// JPY tradeStanRate = 900 (100엔당 900원), buyRate = 945 (100엔당 945원)
			OrderRequest request = new OrderRequest(
				new BigDecimal("200"),
				CurrencyCode.KRW,
				CurrencyCode.JPY
			);
			ExchangeRateResponse rate = ExchangeRateResponse.builder()
				.currencyCode(CurrencyCode.JPY)
				.tradeStanRate(new BigDecimal("900.00"))
				.buyRate(new BigDecimal("945.00"))
				.sellRate(new BigDecimal("855.00"))
				.dateTime(LocalDateTime.now())
				.build();
			given(exchangeRateService.getLatestRate(CurrencyCode.JPY)).willReturn(rate);
			given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

			// when
			OrderCreateResponse response = orderService.createOrder(request);

			// then
			// 200엔 * 945원/100엔 = 1890원
			assertThat(response.fromAmount()).isEqualByComparingTo(new BigDecimal("1890"));
			assertThat(response.toAmount()).isEqualByComparingTo(new BigDecimal("200"));
			assertThat(response.tradeRate()).isEqualByComparingTo(new BigDecimal("945.00"));
		}

		@Test
		@DisplayName("JPY에서 KRW로 매도 주문 시 100엔 단위 환율을 적용한다")
		void shouldApplyRateUnitWhenSellingJpy() {
			// given
			// JPY tradeStanRate = 900 (100엔당 900원), sellRate = 855 (100엔당 855원)
			OrderRequest request = new OrderRequest(
				new BigDecimal("200"),
				CurrencyCode.JPY,
				CurrencyCode.KRW
			);
			ExchangeRateResponse rate = ExchangeRateResponse.builder()
				.currencyCode(CurrencyCode.JPY)
				.tradeStanRate(new BigDecimal("900.00"))
				.buyRate(new BigDecimal("945.00"))
				.sellRate(new BigDecimal("855.00"))
				.dateTime(LocalDateTime.now())
				.build();
			given(exchangeRateService.getLatestRate(CurrencyCode.JPY)).willReturn(rate);
			given(orderRepository.save(any(Order.class))).willAnswer(invocation -> invocation.getArgument(0));

			// when
			OrderCreateResponse response = orderService.createOrder(request);

			// then
			// 200엔 * 855원/100엔 = 1710원
			assertThat(response.toAmount()).isEqualByComparingTo(new BigDecimal("1710"));
			assertThat(response.fromAmount()).isEqualByComparingTo(new BigDecimal("200"));
			assertThat(response.tradeRate()).isEqualByComparingTo(new BigDecimal("855.00"));
		}

		@Test
		@DisplayName("동일한 통화로 주문하면 SAME_CURRENCY 예외가 발생한다")
		void shouldThrowExceptionWhenSameCurrency() {
			// given
			OrderRequest request = new OrderRequest(
				new BigDecimal("100"),
				CurrencyCode.USD,
				CurrencyCode.USD
			);

			// when & then
			assertThatThrownBy(() -> orderService.createOrder(request))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(ErrorCode.SAME_CURRENCY);
		}

		@Test
		@DisplayName("KRW가 포함되지 않은 통화 쌍으로 주문하면 INVALID_CURRENCY_PAIR 예외가 발생한다")
		void shouldThrowExceptionWhenNoKrwInPair() {
			// given
			OrderRequest request = new OrderRequest(
				new BigDecimal("100"),
				CurrencyCode.USD,
				CurrencyCode.EUR
			);

			// when & then
			assertThatThrownBy(() -> orderService.createOrder(request))
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException)ex).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_CURRENCY_PAIR);
		}
	}

	@Nested
	@DisplayName("getOrders 메서드")
	class GetOrders {

		@Test
		@DisplayName("주문 목록을 최신순으로 반환한다")
		void shouldReturnOrdersOrderedByLatest() {
			// given
			LocalDateTime now = LocalDateTime.now();
			List<Order> orders = List.of(
				Order.builder()
					.fromAmount(new BigDecimal("141750"))
					.fromCurrency("KRW")
					.toAmount(new BigDecimal("100"))
					.toCurrency("USD")
					.tradeRate(new BigDecimal("1417.50"))
					.orderedAt(now)
					.build(),
				Order.builder()
					.fromAmount(new BigDecimal("100"))
					.fromCurrency("EUR")
					.toAmount(new BigDecimal("139650"))
					.toCurrency("KRW")
					.tradeRate(new BigDecimal("1396.50"))
					.orderedAt(now.minusMinutes(5))
					.build()
			);
			given(orderRepository.findAllByOrderByOrderedAtDesc()).willReturn(orders);

			// when
			OrderListResponse response = orderService.getOrders();

			// then
			assertThat(response.orderList()).hasSize(2);
			assertThat(response.orderList().get(0).fromCurrency()).isEqualTo("KRW");
			assertThat(response.orderList().get(1).fromCurrency()).isEqualTo("EUR");
		}

		@Test
		@DisplayName("주문이 없으면 빈 목록을 반환한다")
		void shouldReturnEmptyListWhenNoOrders() {
			// given
			given(orderRepository.findAllByOrderByOrderedAtDesc()).willReturn(List.of());

			// when
			OrderListResponse response = orderService.getOrders();

			// then
			assertThat(response.orderList()).isEmpty();
		}
	}
}
