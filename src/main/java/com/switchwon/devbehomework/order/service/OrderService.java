package com.switchwon.devbehomework.order.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.switchwon.devbehomework.common.enums.ErrorCode;
import com.switchwon.devbehomework.common.exception.BusinessException;
import com.switchwon.devbehomework.currency.Currency;
import com.switchwon.devbehomework.currency.RatedCurrency;
import com.switchwon.devbehomework.exchangerate.dto.ExchangeRateResponse;
import com.switchwon.devbehomework.exchangerate.service.ExchangeRateService;
import com.switchwon.devbehomework.order.dto.OrderListResponse;
import com.switchwon.devbehomework.order.dto.OrderRequest;
import com.switchwon.devbehomework.order.dto.OrderResponse;
import com.switchwon.devbehomework.order.entity.ExchangeOrderEntity;
import com.switchwon.devbehomework.order.entity.ExchangeOrderRequestEntity;
import com.switchwon.devbehomework.order.enums.OrderDirection;
import com.switchwon.devbehomework.order.repository.ExchangeOrderRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class OrderService {

	private final ExchangeOrderRepository orderRepository;
	private final ExchangeRateService exchangeRateService;
	private final OrderTransactionService orderTransactionService;
	private final Clock clock;

	@Value("${exchange-rate.rate-freshness-minutes:5}")
	private int rateFreshnessMinutes;

	public OrderResponse createOrder(OrderRequest request) {
		LocalDateTime now = LocalDateTime.now(clock);

		ExchangeOrderRequestEntity orderRequest = orderTransactionService.saveRequest(
			ExchangeOrderRequestEntity.received(
				request.getForexAmount(), request.getFromCurrency(), request.getToCurrency(), now
			)
		);

		try {
			Currency fromCurrency = parseCurrency(request.getFromCurrency());
			Currency toCurrency = parseCurrency(request.getToCurrency());
			validateCurrencyPair(fromCurrency, toCurrency);

			boolean isBuy = fromCurrency == Currency.KRW;
			Currency foreignCurrencyCode = isBuy ? toCurrency : fromCurrency;
			RatedCurrency foreignCurrency = RatedCurrency.valueOf(foreignCurrencyCode.name());

			ExchangeRateResponse rate = exchangeRateService.getLatestRate(Currency.KRW, foreignCurrencyCode);

			if (rate.dateTime().plusMinutes(rateFreshnessMinutes).isBefore(now)) {
				throw new BusinessException(ErrorCode.RATE_STALE);
			}

			OrderDirection direction = isBuy ? OrderDirection.BUY : OrderDirection.SELL;
			ExchangeOrderEntity order = ExchangeOrderEntity.create(
				orderRequest.getId(), direction, foreignCurrency, request.getForexAmount(), rate, now
			);

			orderRequest.markSucceeded(LocalDateTime.now(clock));
			orderTransactionService.saveOrder(order, orderRequest);

			log.info("주문 완료: direction={}, currency={}, forexAmount={}, tradeRate={}",
				direction, foreignCurrency, request.getForexAmount(), order.getTradeRate());

			return toResponse(order);
		} catch (BusinessException e) {
			orderTransactionService.markFailed(orderRequest, e.getErrorCode(), LocalDateTime.now(clock));
			throw e;
		} catch (RuntimeException e) {
			orderTransactionService.markFailed(orderRequest, ErrorCode.INTERNAL_ERROR, LocalDateTime.now(clock));
			throw e;
		}
	}

	public OrderListResponse getOrders(Pageable pageable) {
		Page<OrderResponse> responses = orderRepository.findAll(pageable)
			.map(this::toResponse);
		return OrderListResponse.from(responses);
	}

	private Currency parseCurrency(String code) {
		try {
			return Currency.valueOf(code);
		} catch (IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.UNSUPPORTED_CURRENCY);
		}
	}

	private void validateCurrencyPair(Currency fromCurrency, Currency toCurrency) {
		if (fromCurrency == toCurrency) {
			throw new BusinessException(ErrorCode.SAME_CURRENCY);
		}
		if (fromCurrency != Currency.KRW && toCurrency != Currency.KRW) {
			throw new BusinessException(ErrorCode.INVALID_CURRENCY_PAIR);
		}
	}

	private OrderResponse toResponse(ExchangeOrderEntity order) {
		return OrderResponse.from(order);
	}
}
