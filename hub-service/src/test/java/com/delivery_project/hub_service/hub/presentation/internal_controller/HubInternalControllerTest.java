package com.delivery_project.hub_service.hub.presentation.internal_controller;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.delivery_project.hub_service.hub.application.query_service.HubQueryService;
import com.delivery_project.hub_service.hub.application.result.HubIdsResult;

/**
 * 내부 허브 조회 API 통합 테스트 (컨벤션 §11 — Controller 는 통합 테스트).
 *
 * <p>{@code addFilters = false} 로 보안 필터를 끈다. 내부 API 가 토큰 없이 열려 있다는 것은
 * 실제 필터체인을 태우는 {@code ApiSecurityTest} 가 확인한다.
 */
@WebMvcTest(controllers = HubInternalController.class)
@AutoConfigureMockMvc(addFilters = false)
class HubInternalControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private HubQueryService hubQueryService;

	@Test
	@DisplayName("전체 허브 ID 를 조회하면 200 과 ID 배열을 돌려준다")
	void getHubIdsReturnsAllIds() throws Exception {
		// given
		List<UUID> hubIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
		when(hubQueryService.getHubIds()).thenReturn(new HubIdsResult(hubIds));

		// when & then
		mockMvc.perform(get("/internal/v1/hubs/ids"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.hubIds", hasSize(3)))
				.andExpect(jsonPath("$.data.hubIds", containsInAnyOrder(
						hubIds.get(0).toString(), hubIds.get(1).toString(), hubIds.get(2).toString())));
	}

	@Test
	@DisplayName("허브가 하나도 없으면 404 가 아니라 200 과 빈 배열이다")
	void getHubIdsReturnsEmptyArrayWhenNoHub() throws Exception {
		// given: 0건은 에러가 아니다 (00_common.md)
		when(hubQueryService.getHubIds()).thenReturn(new HubIdsResult(List.of()));

		// when & then
		mockMvc.perform(get("/internal/v1/hubs/ids"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.hubIds", hasSize(0)));
	}

	@Test
	@DisplayName("/ids 는 단건 조회의 경로 변수로 잡히지 않는다")
	void idsPathDoesNotFallIntoSingleHubMapping() throws Exception {
		// given: /{hubId} 와 같은 자리를 다툰다. 여기로 새면 "ids" 를 UUID 로 변환하다 400 이 난다.
		when(hubQueryService.getHubIds()).thenReturn(new HubIdsResult(List.of()));

		// when
		mockMvc.perform(get("/internal/v1/hubs/ids"))
				.andExpect(status().isOk());

		// then
		verify(hubQueryService).getHubIds();
		verify(hubQueryService, never()).getHubSummary(any());
	}
}
