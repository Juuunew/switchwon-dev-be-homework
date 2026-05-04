package com.switchwon.devbehomework.order.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;

public record OrderListResponse(
	List<OrderResponse> orderList,
	@Schema(example = "1") int totalPages,
	@Schema(example = "1") long totalElements,
	@Schema(example = "0") int page,
	@Schema(example = "20") int size
) {

	public static OrderListResponse from(Page<OrderResponse> page) {
		return new OrderListResponse(
			page.getContent(),
			page.getTotalPages(),
			page.getTotalElements(),
			page.getNumber(),
			page.getSize()
		);
	}
}
