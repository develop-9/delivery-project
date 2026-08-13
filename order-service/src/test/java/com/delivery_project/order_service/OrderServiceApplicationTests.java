package com.delivery_project.order_service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 애플리케이션 컨텍스트 스모크 테스트.
 *
 * <p>src/test/resources/application.yaml 의 {@code smoke} 프로파일 덕분에
 * DB·Redis·Eureka·Zipkin 이 하나도 없어도 통과한다.
 * 이 테스트가 깨지면 빈 등록·자동설정 어딘가가 망가진 것이다.
 */
@SpringBootTest
@ActiveProfiles("smoke")
class OrderServiceApplicationTests {

	@Test
	@DisplayName("인프라 없이도 애플리케이션 컨텍스트가 로딩된다")
	void contextLoads() {
		// given & when: @SpringBootTest 가 컨텍스트를 띄운다
		// then: 예외 없이 로딩되면 성공
	}
}
