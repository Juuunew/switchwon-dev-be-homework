package com.switchwon.devbehomework.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.switchwon.devbehomework.order.entity.ExchangeOrderEntity;

public interface ExchangeOrderRepository extends JpaRepository<ExchangeOrderEntity, Long> {

	List<ExchangeOrderEntity> findAllByOrderByCreatedAtDesc();
}
