package com.switchwon.devbehomework.scheduler;

import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.switchwon.devbehomework.common.enums.ErrorCode;
import com.switchwon.devbehomework.common.exception.BusinessException;
import com.switchwon.devbehomework.exchangerate.collection.ExchangeRateCollectionScheduler;
import com.switchwon.devbehomework.exchangerate.collection.ExchangeRateCollector;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateCollectionScheduler 단위 테스트")
class ExchangeRateSchedulerTest {

	@InjectMocks
	private ExchangeRateCollectionScheduler scheduler;

	@Mock
	private ExchangeRateCollector exchangeRateCollector;

	@Test
	@DisplayName("collect 호출 시 collector.collectAll을 실행한다")
	void shouldCallCollectAll() {
		// when
		scheduler.collect();

		// then
		then(exchangeRateCollector).should().collectAll();
	}

	@Test
	@DisplayName("collectAll에서 예외 발생 시 예외를 전파하지 않는다")
	void shouldNotPropagateExceptionWhenCollectFails() {
		// given
		willThrow(new BusinessException(ErrorCode.EXTERNAL_API_ERROR))
			.given(exchangeRateCollector).collectAll();

		// when & then (예외 발생하지 않음)
		scheduler.collect();
	}
}
