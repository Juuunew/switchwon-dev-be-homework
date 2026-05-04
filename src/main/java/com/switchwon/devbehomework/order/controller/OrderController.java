package com.switchwon.devbehomework.order.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.switchwon.devbehomework.common.dto.ApiResponse;
import com.switchwon.devbehomework.order.dto.OrderListResponse;
import com.switchwon.devbehomework.order.dto.OrderRequest;
import com.switchwon.devbehomework.order.dto.OrderResponse;
import com.switchwon.devbehomework.order.service.OrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/order")
@RestController
public class OrderController {

	private final OrderService orderService;

	@PostMapping
	public ApiResponse<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
		return ApiResponse.success(orderService.createOrder(request));
	}

	@GetMapping("/list")
	public ApiResponse<OrderListResponse> getOrders(
		@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		return ApiResponse.success(orderService.getOrders(pageable));
	}
}
