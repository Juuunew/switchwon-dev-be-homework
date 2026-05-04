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
import com.switchwon.devbehomework.currency.CurrencyCode;
import com.switchwon.devbehomework.currency.ForeignCurrency;
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
			CurrencyCode fromCurrency = parseCurrency(request.getFromCurrency());
			CurrencyCode toCurrency = parseCurrency(request.getToCurrency());
			validateCurrencyPair(fromCurrency, toCurrency);

			boolean isBuy = fromCurrency == CurrencyCode.KRW;
			ForeignCurrency foreignCurrency = ForeignCurrency.from(
				isBuy ? toCurrency : fromCurrency
			);

			ExchangeRateResponse rate = exchangeRateService.getLatestRate(foreignCurrency);

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

	private CurrencyCode parseCurrency(String code) {
		try {
			return CurrencyCode.valueOf(code);
		} catch (IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.UNSUPPORTED_CURRENCY);
		}
	}

	private void validateCurrencyPair(CurrencyCode fromCurrency, CurrencyCode toCurrency) {
		if (fromCurrency == toCurrency) {
			throw new BusinessException(ErrorCode.SAME_CURRENCY);
		}
		if (fromCurrency != CurrencyCode.KRW && toCurrency != CurrencyCode.KRW) {
			throw new BusinessException(ErrorCode.INVALID_CURRENCY_PAIR);
		}
	}

	private OrderResponse toResponse(ExchangeOrderEntity order) {
		boolean isBuy = order.getDirection() == OrderDirection.BUY;
		if (isBuy) {
			return new OrderResponse(
				order.getId(),
				order.getKrwAmount(), "KRW",
				order.getForexAmount(), order.getCurrency().name(),
				order.getTradeRate(), order.getCreatedAt()
			);
		}
		return new OrderResponse(
			order.getId(),
			order.getForexAmount(), order.getCurrency().name(),
			order.getKrwAmount(), "KRW",
			order.getTradeRate(), order.getCreatedAt()
		);
	}
}
