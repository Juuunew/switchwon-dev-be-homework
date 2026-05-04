package com.switchwon.devbehomework.exchangerate.collection;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class ExchangeRateInitialCollector implements ApplicationRunner {

	private final ExchangeRateCollector exchangeRateCollector;

	@Value("${exchange-rate.initial-collection.enabled:true}")
	private boolean enabled;

	@Override
	public void run(ApplicationArguments args) {
		if (!enabled) {
			log.info("초기 환율 수집이 비활성화되어 있습니다.");
			return;
		}

		try {
			exchangeRateCollector.collectAll();
		} catch (Exception ex) {
			log.error("초기 환율 수집 중 예기치 않은 오류가 발생했습니다.", ex);
		}
	}
}
