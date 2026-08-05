package com.delivery_project.hub_service.hub.presentation.api_controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.delivery_project.hub_service.global.exception.BusinessException;
import com.delivery_project.hub_service.global.exception.ErrorCode;
import com.delivery_project.hub_service.hub.application.command_service.HubCommandService;
import com.delivery_project.hub_service.hub.application.query_service.HubQueryService;
import com.delivery_project.hub_service.hub.application.result.HubCreateResult;
import com.delivery_project.hub_service.hub.application.result.HubDeleteResult;
import com.delivery_project.hub_service.hub.application.result.HubDetailResult;
import com.delivery_project.hub_service.hub.domain.entity.HubType;

/**
 * 허브 API 통합 테스트 (컨벤션 §11 — Controller 는 통합 테스트).
 *
 * <p>{@code addFilters = false} 로 보안 필터를 끈다. 권한 판정은 컨트롤러가 아니라 서비스의
 * {@code @PreAuthorize} 가 하고 그 서비스는 여기서 대역이라, 필터를 켜 봐야 검증되는 게 없고
 * 토큰 발급 절차만 늘어난다. 권한 규칙은 서비스 단위 테스트가 맡는다.
 */
@WebMvcTest(controllers = HubApiController.class)
@AutoConfigureMockMvc(addFilters = false)
class HubApiControllerTest {

	private static final UUID HUB_ID = UUID.randomUUID();

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private HubCommandService hubCommandService;

	@MockitoBean
	private HubQueryService hubQueryService;

	@Test
	@DisplayName("허브를 생성하면 201 과 생성된 허브를 돌려준다")
	void createReturnsCreated() throws Exception {
		// given
		when(hubCommandService.create(any(), any())).thenReturn(new HubCreateResult(
				HUB_ID, "경기 남부 센터", "경기도 이천시 부발읍 경충대로 2091",
				BigDecimal.valueOf(37.272), BigDecimal.valueOf(127.435),
				HubType.MAIN, HUB_ID, "경기 남부 센터", Instant.now(), Instant.now()));

		String body = """
				{
				  "name": "경기 남부 센터",
				  "address": "경기도 이천시 부발읍 경충대로 2091",
				  "latitude": 37.2720000,
				  "longitude": 127.4350000,
				  "hubType": "MAIN"
				}""";

		// when & then
		mockMvc.perform(post("/api/v1/hubs").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.hubId").value(HUB_ID.toString()))
				// MAIN 이므로 상위 허브가 자기 자신이다 (D1)
				.andExpect(jsonPath("$.data.parentHubId").value(HUB_ID.toString()));
	}

	@Test
	@DisplayName("필수 값이 빠지면 400 INVALID_INPUT_VALUE 다")
	void createRejectsInvalidRequest() throws Exception {
		// given: 위경도는 API 레벨 필수다 (D5)
		String body = """
				{
				  "name": "경기 남부 센터",
				  "address": "경기도 이천시",
				  "hubType": "MAIN"
				}""";

		// when & then
		mockMvc.perform(post("/api/v1/hubs").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.errorCode").value("INVALID_INPUT_VALUE"));
	}

	@Test
	@DisplayName("단건 조회 응답에는 감사 필드가 없다")
	void getHubOmitsAuditFields() throws Exception {
		// given
		when(hubQueryService.getHub(HUB_ID)).thenReturn(new HubDetailResult(
				HUB_ID, "부산광역시 센터", "부산광역시 동구 중앙대로 206",
				BigDecimal.valueOf(35.1156), BigDecimal.valueOf(129.041),
				HubType.SUB, UUID.randomUUID(), "대구광역시 센터"));

		// when & then
		mockMvc.perform(get("/api/v1/hubs/{hubId}", HUB_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name").value("부산광역시 센터"))
				.andExpect(jsonPath("$.data.parentHubName").value("대구광역시 센터"))
				.andExpect(jsonPath("$.data.createdAt").doesNotExist())
				.andExpect(jsonPath("$.data.updatedAt").doesNotExist());
	}

	@Test
	@DisplayName("없는 허브를 조회하면 404 HUB_NOT_FOUND 다")
	void getHubReturnsNotFound() throws Exception {
		// given
		when(hubQueryService.getHub(HUB_ID)).thenThrow(new BusinessException(ErrorCode.HUB_NOT_FOUND));

		// when & then
		mockMvc.perform(get("/api/v1/hubs/{hubId}", HUB_ID))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.errorCode").value("HUB_NOT_FOUND"));
	}

	@Test
	@DisplayName("허용되지 않은 size 는 에러가 아니라 10 으로 보정되어 응답된다")
	void searchCorrectsDisallowedSize() throws Exception {
		// given: 서비스에는 이미 보정된 Pageable 이 전달된다
		when(hubQueryService.searchHubs(any(), any())).thenAnswer(invocation -> {
			Pageable pageable = invocation.getArgument(1);
			return new PageImpl<>(List.of(), pageable, 0);
		});

		// when & then
		mockMvc.perform(get("/api/v1/hubs").param("size", "17"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.size").value(10))
				.andExpect(jsonPath("$.data.sort").value("createdAt,desc"))
				// 결과 0건도 에러가 아니다
				.andExpect(jsonPath("$.data.content").isArray())
				.andExpect(jsonPath("$.data.totalElements").value(0));
	}

	@Test
	@DisplayName("삭제 응답에는 함께 지워진 이동정보 수가 담긴다")
	void deleteReturnsCascadedHubRouteCount() throws Exception {
		// given: D6 로 연관 구간 2개가 함께 논리 삭제됐다
		UUID callerId = UUID.randomUUID();
		when(hubCommandService.delete(any(), eq(HUB_ID))).thenReturn(new HubDeleteResult(
				HUB_ID, "인천광역시 센터", 2, Instant.now(), callerId));

		// when & then
		mockMvc.perform(delete("/api/v1/hubs/{hubId}", HUB_ID))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.deletedHubRouteCount").value(2))
				.andExpect(jsonPath("$.data.deletedBy").value(callerId.toString()));
	}
}
