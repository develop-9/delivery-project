package com.delivery_project.hub_service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * actuator 노출 설정 통합 테스트.
 *
 * <p>compose healthcheck 가 인증 없이 {@code /actuator/health} 를 호출하므로,
 * 미인증 접근이 200 을 반환하는 것이 이 서비스의 계약이다. JWT 필터가 들어온 뒤에도
 * 이 계약은 유지되어야 한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ActuatorHealthTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("health 엔드포인트는 인증 없이 접근 가능하고 UP 을 반환한다")
	void healthEndpointIsExposedWithoutAuthentication() throws Exception {
		// given: 테스트 설정이 DB·Redis 헬스 인디케이터를 꺼 인프라와 무관하게 동작한다

		// when & then
		mockMvc.perform(get("/actuator/health"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	@DisplayName("노출 목록에 없는 엔드포인트는 404 를 반환한다")
	void unexposedEndpointIsNotFound() throws Exception {
		// given: management.endpoints.web.exposure.include 는 health,info 뿐이다

		// when & then
		mockMvc.perform(get("/actuator/env"))
			.andExpect(status().isNotFound());
	}
}
