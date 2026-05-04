package com.switchwon.devbehomework.exchangerate.collection;

import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.switchwon.devbehomework.common.enums.ErrorCode;
import com.switchwon.devbehomework.common.exception.BusinessException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateInitialCollector 단위 테스트")
class ExchangeRateInitialCollectorTest {

	@InjectMocks
	private ExchangeRateInitialCollector initialCollector;

	@Mock
	private ExchangeRateCollector exchangeRateCollector;

	@Test
	@DisplayName("초기 수집이 활성화되어 있으면 collectAll을 실행한다")
	void shouldCollectWhenEnabled() {
		// given
		ReflectionTestUtils.setField(initialCollector, "enabled", true);

		// when
		initialCollector.run(null);

		// then
		then(exchangeRateCollector).should().collectAll();
	}

	@Test
	@DisplayName("초기 수집이 비활성화되어 있으면 collectAll을 실행하지 않는다")
	void shouldNotCollectWhenDisabled() {
		// given
		ReflectionTestUtils.setField(initialCollector, "enabled", false);

		// when
		initialCollector.run(null);

		// then
		then(exchangeRateCollector).should(never()).collectAll();
	}

	@Test
	@DisplayName("초기 수집 중 예외 발생 시 예외를 전파하지 않는다")
	void shouldNotPropagateExceptionWhenInitialCollectFails() {
		// given
		ReflectionTestUtils.setField(initialCollector, "enabled", true);
		willThrow(new BusinessException(ErrorCode.EXTERNAL_API_ERROR))
			.given(exchangeRateCollector).collectAll();

		// when & then (예외 발생하지 않음)
		initialCollector.run(null);
	}
}
