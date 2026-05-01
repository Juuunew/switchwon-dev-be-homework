package com.switchwon.devbehomework.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.switchwon.devbehomework.order.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

	List<Order> findAllByOrderByOrderedAtDesc();
}
