package com.switchwon.devbehomework.order.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.switchwon.devbehomework.common.enums.CurrencyCode;
import com.switchwon.devbehomework.common.enums.ErrorCode;
import com.switchwon.devbehomework.common.exception.BusinessException;
import com.switchwon.devbehomework.exchangerate.dto.ExchangeRateResponse;
import com.switchwon.devbehomework.exchangerate.service.ExchangeRateService;
import com.switchwon.devbehomework.order.dto.OrderCreateResponse;
import com.switchwon.devbehomework.order.dto.OrderDetailResponse;
import com.switchwon.devbehomework.order.dto.OrderListResponse;
import com.switchwon.devbehomework.order.dto.OrderRequest;
import com.switchwon.devbehomework.order.entity.Order;
import com.switchwon.devbehomework.order.repository.OrderRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class OrderService {

	private final OrderRepository orderRepository;
	private final ExchangeRateService exchangeRateService;

	@Transactional
	public OrderCreateResponse createOrder(OrderRequest request) {
		validateCurrencyPair(request.getFromCurrency(), request.getToCurrency());

		boolean isBuy = request.getFromCurrency() == CurrencyCode.KRW;
		CurrencyCode foreignCurrency = isBuy ? request.getToCurrency() : request.getFromCurrency();
		ExchangeRateResponse rate = exchangeRateService.getLatestRate(foreignCurrency);

		BigDecimal tradeRate;
		BigDecimal fromAmount;
		BigDecimal toAmount;
		int rateUnit = foreignCurrency.getForeignCurrency().getRateUnit();

		if (isBuy) {
			tradeRate = rate.buyRate();
			toAmount = request.getForexAmount();
			fromAmount = request.getForexAmount().multiply(tradeRate)
				.divide(BigDecimal.valueOf(rateUnit), 0, RoundingMode.FLOOR);
		} else {
			tradeRate = rate.sellRate();
			fromAmount = request.getForexAmount();
			toAmount = request.getForexAmount().multiply(tradeRate)
				.divide(BigDecimal.valueOf(rateUnit), 0, RoundingMode.FLOOR);
		}

		Order order = Order.of(fromAmount, request.getFromCurrency().name(),
			toAmount, request.getToCurrency().name(), tradeRate, LocalDateTime.now());

		orderRepository.save(order);
		return OrderCreateResponse.from(order);
	}

	public OrderListResponse getOrders() {
		List<Order> orders = orderRepository.findAllByOrderByOrderedAtDesc();
		List<OrderDetailResponse> responses = orders.stream()
			.map(OrderDetailResponse::from)
			.toList();
		return OrderListResponse.from(responses);
	}

	private void validateCurrencyPair(CurrencyCode fromCurrency, CurrencyCode toCurrency) {
		if (fromCurrency == toCurrency) {
			throw new BusinessException(ErrorCode.SAME_CURRENCY);
		}
		if (fromCurrency != CurrencyCode.KRW && toCurrency != CurrencyCode.KRW) {
			throw new BusinessException(ErrorCode.INVALID_CURRENCY_PAIR);
		}
	}
}
