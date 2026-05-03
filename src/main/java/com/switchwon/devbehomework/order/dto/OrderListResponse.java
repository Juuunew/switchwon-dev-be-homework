package com.switchwon.devbehomework.order.dto;

import java.util.List;

public record OrderListResponse(List<OrderResponse> orderList) {

	public static OrderListResponse from(List<OrderResponse> orders) {
		return new OrderListResponse(orders);
	}
}
