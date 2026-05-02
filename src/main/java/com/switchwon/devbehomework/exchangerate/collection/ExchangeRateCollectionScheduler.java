package com.switchwon.devbehomework.exchangerate.collection;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class ExchangeRateCollectionScheduler {

	private final ExchangeRateCollector exchangeRateCollector;

	@Scheduled(
		fixedDelayString = "${exchange-rate.schedule.fixed-delay}",
		initialDelayString = "${exchange-rate.schedule.initial-delay:0}"
	)
	public void collect() {
		try {
			exchangeRateCollector.collectAll();
		} catch (Exception ex) {
			log.error("환율 수집 중 예기치 않은 오류 발생", ex);
		}
	}
}
