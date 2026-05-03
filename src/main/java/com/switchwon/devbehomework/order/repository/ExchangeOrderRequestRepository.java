package com.switchwon.devbehomework.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.switchwon.devbehomework.order.entity.ExchangeOrderRequestEntity;

public interface ExchangeOrderRequestRepository extends JpaRepository<ExchangeOrderRequestEntity, Long> {
}
