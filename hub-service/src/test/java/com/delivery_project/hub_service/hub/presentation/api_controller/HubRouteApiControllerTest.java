package com.delivery_project.hub_service.hub.presentation.api_controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.delivery_project.hub_service.global.exception.BusinessException;
import com.delivery_project.hub_service.global.exception.ErrorCode;
import com.delivery_project.hub_service.hub.application.command_service.HubRouteCommandService;
import com.delivery_project.hub_service.hub.application.query.HubRouteSearchQuery;
import com.delivery_project.hub_service.hub.application.query_service.HubRouteQueryService;

/**
 * 이동정보 검색 API 통합 테스트 (컨벤션 §11 — Controller 는 통합 테스트).
 *
 * <p>{@code addFilters = false} 로 보안 필터를 끈다. 권한 판정은 컨트롤러가 아니라 서비스의
 * {@code @PreAuthorize} 가 하고 그 서비스는 여기서 대역이라, 필터를 켜 봐야 검증되는 게 없다.
 *
 * <p>여기서 보는 것은 <b>쿼리 파라미터가 Query 로 옮겨지는지</b>와 정렬 보정이다.
 * 컨트롤러는 더 이상 검색 조건을 직접 조립하지 않는다.
 */
@WebMvcTest(controllers = HubRouteApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class HubRouteApiControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private HubRouteCommandService hubRouteCommandService;

	@MockitoBean
	private HubRouteQueryService hubRouteQueryService;

	@Test
	@DisplayName("거리 범위 파라미터가 Query 로 그대로 전달된다")
	void bindsDistanceRangeToQuery() throws Exception {
		// given
		UUID departureHubId = UUID.randomUUID();
		stubEmptyPage();

		// when
		mockMvc.perform(get("/api/v1/hub-routes")
						.param("departureHubId", departureHubId.toString())
						.param("minDistanceKm", "50.00")
						.param("maxDistanceKm", "200.00")
						.param("minDurationMin", "30"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true));

		// then
		ArgumentCaptor<HubRouteSearchQuery> captor = ArgumentCaptor.forClass(HubRouteSearchQuery.class);
		verify(hubRouteQueryService).searchHubRoutes(captor.capture(), any());

		HubRouteSearchQuery query = captor.getValue();
		assertThat(query.departureHubId()).isEqualTo(departureHubId);
		assertThat(query.minDistanceKm()).isEqualByComparingTo(new BigDecimal("50.00"));
		assertThat(query.maxDistanceKm()).isEqualByComparingTo(new BigDecimal("200.00"));
		assertThat(query.minDurationMin()).isEqualTo(30);
		assertThat(query.maxDurationMin()).isNull();
	}

	@Test
	@DisplayName("distanceKm 정렬이 허용값에 들어와 그대로 적용된다")
	void allowsDistanceSort() throws Exception {
		// given
		stubEmptyPage();

		// when & then
		mockMvc.perform(get("/api/v1/hub-routes").param("sort", "distanceKm").param("direction", "asc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.sort").value("distanceKm,asc"));
	}

	@Test
	@DisplayName("허용되지 않은 정렬 키는 400 이 아니라 createdAt 으로 보정된다")
	void correctsDisallowedSort() throws Exception {
		// given
		stubEmptyPage();

		// when & then
		mockMvc.perform(get("/api/v1/hub-routes").param("sort", "distance"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.sort").value("createdAt,desc"))
				.andExpect(jsonPath("$.data.content").isArray())
				.andExpect(jsonPath("$.data.totalElements").value(0));
	}

	@Test
	@DisplayName("거리 범위가 역전되면 400 INVALID_INPUT_VALUE 다")
	void rejectsInvertedDistanceRange() throws Exception {
		// given: 역전은 두 필드를 함께 봐야 알 수 있어 서비스가 판정한다
		when(hubRouteQueryService.searchHubRoutes(any(), any()))
				.thenThrow(new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

		// when & then
		mockMvc.perform(get("/api/v1/hub-routes")
						.param("minDistanceKm", "200.00")
						.param("maxDistanceKm", "50.00"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.errorCode").value("INVALID_INPUT_VALUE"));
	}

	@Test
	@DisplayName("거리가 음수면 서비스까지 가지 않고 400 INVALID_INPUT_VALUE 다")
	void rejectsNegativeDistance() throws Exception {
		// when & then
		mockMvc.perform(get("/api/v1/hub-routes").param("minDistanceKm", "-1.00"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.errorCode").value("INVALID_INPUT_VALUE"));
	}

	private void stubEmptyPage() {
		when(hubRouteQueryService.searchHubRoutes(any(), any())).thenAnswer(invocation -> {
			Pageable pageable = invocation.getArgument(1);
			return new PageImpl<>(List.of(), pageable, 0);
		});
	}
}
