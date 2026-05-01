package com.switchwon.devbehomework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI openApi() {
		return new OpenAPI()
			.info(new Info()
				.title("실시간 환율 기반 외환 주문 API")
				.version("1.0")
				.description("외부 환율 API를 통해 1분마다 환율을 수집하고 외화 매수/매도 주문을 처리하는 시스템"));
	}
}
