package com.switchwon.devbehomework.order.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.switchwon.devbehomework.order.entity.ExchangeOrderEntity;

public interface ExchangeOrderRepository extends JpaRepository<ExchangeOrderEntity, Long> {

	Page<ExchangeOrderEntity> findAll(Pageable pageable);
}
