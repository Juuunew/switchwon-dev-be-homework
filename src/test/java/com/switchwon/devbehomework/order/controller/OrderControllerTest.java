package com.switchwon.devbehomework.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.switchwon.devbehomework.order.dto.OrderCreateResponse;
import com.switchwon.devbehomework.order.dto.OrderDetailResponse;
import com.switchwon.devbehomework.order.dto.OrderListResponse;
import com.switchwon.devbehomework.order.dto.OrderRequest;
import com.switchwon.devbehomework.order.service.OrderService;

@WebMvcTest(OrderController.class)
@DisplayName("OrderController 테스트")
class OrderControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private OrderService orderService;

	@Nested
	@DisplayName("POST /order")
	class CreateOrder {

		@Test
		@DisplayName("유효한 매수 주문 요청 시 주문 결과를 반환한다")
		void shouldCreateBuyOrder() throws Exception {
			// given
			LocalDateTime now = LocalDateTime.now();
			OrderCreateResponse response = new OrderCreateResponse(
				new BigDecimal("141750"),
				"KRW",
				new BigDecimal("100"),
				"USD",
				new BigDecimal("1417.50"),
				now
			);
			given(orderService.createOrder(any())).willReturn(response);

			String requestBody = "{\"forexAmount\":100,\"fromCurrency\":\"KRW\",\"toCurrency\":\"USD\"}";

			// when & then
			mockMvc.perform(post("/order")
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("OK"))
				.andExpect(jsonPath("$.returnObject.fromCurrency").value("KRW"))
				.andExpect(jsonPath("$.returnObject.toCurrency").value("USD"))
				.andExpect(jsonPath("$.returnObject.fromAmount").value(141750))
				.andExpect(jsonPath("$.returnObject.toAmount").value(100))
				.andExpect(jsonPath("$.returnObject.id").doesNotExist());
		}

		@Test
		@DisplayName("forexAmount가 null이면 400 에러를 반환한다")
		void shouldReturn400WhenForexAmountIsNull() throws Exception {
			// given
			String requestBody = "{\"fromCurrency\":\"KRW\",\"toCurrency\":\"USD\"}";

			// when & then
			mockMvc.perform(post("/order")
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("400"));
		}

		@Test
		@DisplayName("forexAmount가 0 이하이면 400 에러를 반환한다")
		void shouldReturn400WhenForexAmountIsNotPositive() throws Exception {
			// given
			String requestBody = "{\"forexAmount\":-100,\"fromCurrency\":\"KRW\",\"toCurrency\":\"USD\"}";

			// when & then
			mockMvc.perform(post("/order")
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("400"));
		}

		@Test
		@DisplayName("fromCurrency가 null이면 400 에러를 반환한다")
		void shouldReturn400WhenFromCurrencyIsNull() throws Exception {
			// given
			String requestBody = "{\"forexAmount\":100,\"toCurrency\":\"USD\"}";

			// when & then
			mockMvc.perform(post("/order")
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("400"));
		}

		@Test
		@DisplayName("잘못된 enum 값이 오면 400 에러를 반환한다")
		void shouldReturn400WhenInvalidEnumValue() throws Exception {
			// given
			String requestBody = "{\"forexAmount\":100,\"fromCurrency\":\"GBP\",\"toCurrency\":\"USD\"}";

			// when & then
			mockMvc.perform(post("/order")
					.contentType(MediaType.APPLICATION_JSON)
					.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("400"));
		}
	}

	@Nested
	@DisplayName("GET /order/list")
	class GetOrders {

		@Test
		@DisplayName("주문 목록을 성공적으로 반환한다")
		void shouldReturnOrderList() throws Exception {
			// given
			LocalDateTime now = LocalDateTime.now();
			List<OrderDetailResponse> orders = List.of(
				new OrderDetailResponse(
					1L,
					new BigDecimal("141750"),
					"KRW",
					new BigDecimal("100"),
					"USD",
					new BigDecimal("1417.50"),
					now
				)
			);
			given(orderService.getOrders()).willReturn(OrderListResponse.from(orders));

			// when & then
			mockMvc.perform(get("/order/list"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code").value("OK"))
				.andExpect(jsonPath("$.returnObject.orderList").isArray())
				.andExpect(jsonPath("$.returnObject.orderList[0].id").value(1))
				.andExpect(jsonPath("$.returnObject.orderList[0].fromCurrency").value("KRW"));
		}

		@Test
		@DisplayName("주문이 없으면 빈 목록을 반환한다")
		void shouldReturnEmptyOrderList() throws Exception {
			// given
			given(orderService.getOrders()).willReturn(OrderListResponse.from(List.of()));

			// when & then
			mockMvc.perform(get("/order/list"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.returnObject.orderList").isEmpty());
		}
	}
}
