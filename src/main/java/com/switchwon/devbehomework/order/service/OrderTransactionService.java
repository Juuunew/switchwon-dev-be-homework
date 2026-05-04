package com.switchwon.devbehomework.order.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.switchwon.devbehomework.common.enums.ErrorCode;
import com.switchwon.devbehomework.order.entity.ExchangeOrderEntity;
import com.switchwon.devbehomework.order.entity.ExchangeOrderRequestEntity;
import com.switchwon.devbehomework.order.repository.ExchangeOrderRepository;
import com.switchwon.devbehomework.order.repository.ExchangeOrderRequestRepository;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class OrderTransactionService {

	private final ExchangeOrderRequestRepository requestRepository;
	private final ExchangeOrderRepository orderRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public ExchangeOrderRequestEntity saveRequest(ExchangeOrderRequestEntity request) {
		return requestRepository.save(request);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public ExchangeOrderEntity saveOrder(ExchangeOrderEntity order, ExchangeOrderRequestEntity request) {
		orderRepository.save(order);
		requestRepository.save(request);
		return order;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markFailed(ExchangeOrderRequestEntity request, ErrorCode errorCode,
		java.time.LocalDateTime completedAt) {
		request.markFailed(errorCode, completedAt);
		requestRepository.save(request);
	}
}
