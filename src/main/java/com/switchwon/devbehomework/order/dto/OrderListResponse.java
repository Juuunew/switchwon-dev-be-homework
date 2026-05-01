package com.switchwon.devbehomework.order.dto;

import java.util.List;

public record OrderListResponse(List<OrderDetailResponse> orderList) {

	public static OrderListResponse from(List<OrderDetailResponse> orders) {
		return new OrderListResponse(orders);
	}
}
