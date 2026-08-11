package com.delivery_project.hub_service.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.delivery_project.hub_service.global.security.JwtAuthenticationFilter;
import com.delivery_project.hub_service.global.security.JwtTokenParser;
import com.delivery_project.hub_service.global.security.Role;
import com.delivery_project.hub_service.hub.application.command.HubCreateCommand;
import com.delivery_project.hub_service.hub.application.command_service.HubCommandService;
import com.delivery_project.hub_service.hub.application.query.HubSummaryQuery;
import com.delivery_project.hub_service.hub.application.query_service.HubQueryService;
import com.delivery_project.hub_service.hub.application.result.HubCreateResult;
import com.delivery_project.hub_service.hub.application.result.HubIdsResult;
import com.delivery_project.hub_service.hub.application.result.HubSummaryResult;
import com.delivery_project.hub_service.hub.domain.entity.HubType;
import com.delivery_project.hub_service.hub.presentation.api_controller.HubApiController;
import com.delivery_project.hub_service.hub.presentation.internal_controller.HubInternalController;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.mockito.ArgumentCaptor;

/**
 * 보안 필터체인 통합 테스트.
 *
 * <p>{@code HubApiControllerTest} 는 {@code addFilters = false} 로 보안을 끄고 매핑·응답만 본다.
 * 여기서는 반대로 <b>실제 {@link SecurityConfig} 와 {@link JwtAuthenticationFilter} 를 태워</b>
 * 토큰이 없거나 망가졌을 때 정말 막히는지, 그리고 내부 API 가 열려 있는지를 확인한다.
 *
 * <p>{@code @PreAuthorize} 로 거는 {@code MASTER} 판정은 여기서 검증되지 않는다 —
 * 서비스가 대역이라 애너테이션이 붙은 실제 빈이 없다. 그쪽은
 * {@code HubCommandServiceAuthorizationTest} 가 맡는다.
 */
@WebMvcTest(controllers = {HubApiController.class, HubInternalController.class})
@Import({SecurityConfig.class, JwtTokenParser.class,
		JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class})
class ApiSecurityTest {

	private static final String CREATE_BODY = """
			{
			  "name": "경기 남부 센터",
			  "address": "경기도 이천시 부발읍 경충대로 2091",
			  "latitude": 37.2720000,
			  "longitude": 127.4350000,
			  "hubType": "MAIN"
			}""";

	@Autowired
	private MockMvc mockMvc;

	@Value("${jwt.secret}")
	private String secret;

	@MockitoBean
	private HubCommandService hubCommandService;

	@MockitoBean
	private HubQueryService hubQueryService;

	@Test
	@DisplayName("토큰 없이 외부 API 를 호출하면 401 AUTH_UNAUTHORIZED 다")
	void rejectsRequestWithoutToken() throws Exception {
		// given: Authorization 헤더를 아예 보내지 않는다

		// when & then
		mockMvc.perform(get("/api/v1/hubs/{hubId}", UUID.randomUUID()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.errorCode").value("AUTH_UNAUTHORIZED"));
	}

	@Test
	@DisplayName("서명이 맞지 않는 토큰은 401 이다")
	void rejectsTokenWithWrongSignature() throws Exception {
		// given: 다른 키로 서명한 토큰
		String forged = token(UUID.randomUUID(), Role.MASTER,
				"another-secret-that-is-long-enough-for-hmac-sha256!!", Instant.now().plus(1, ChronoUnit.HOURS));

		// when & then
		mockMvc.perform(get("/api/v1/hubs/{hubId}", UUID.randomUUID())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + forged))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.errorCode").value("AUTH_UNAUTHORIZED"));
	}

	@Test
	@DisplayName("만료된 토큰은 401 이다")
	void rejectsExpiredToken() throws Exception {
		// given: 이미 지난 시각으로 만료된 토큰
		String expired = token(UUID.randomUUID(), Role.MASTER, secret,
				Instant.now().minus(1, ChronoUnit.HOURS));

		// when & then
		mockMvc.perform(get("/api/v1/hubs/{hubId}", UUID.randomUUID())
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.errorCode").value("AUTH_UNAUTHORIZED"));
	}

	@Test
	@DisplayName("Bearer 접두사가 없으면 인증되지 않아 401 이다")
	void rejectsTokenWithoutBearerPrefix() throws Exception {
		// given
		String raw = token(UUID.randomUUID(), Role.MASTER, secret, Instant.now().plus(1, ChronoUnit.HOURS));

		// when & then
		mockMvc.perform(get("/api/v1/hubs/{hubId}", UUID.randomUUID())
						.header(HttpHeaders.AUTHORIZATION, raw))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("내부 API 는 토큰 없이도 열려 있다")
	void internalApiIsOpenWithoutToken() throws Exception {
		// given: 게이트웨이 라우팅이 외부 인입을 막는 것에 의존한다 (03_internal.md)
		UUID hubId = UUID.randomUUID();
		when(hubQueryService.getHubSummary(new HubSummaryQuery(hubId))).thenReturn(new HubSummaryResult(
				hubId, "대구광역시 센터", "대구광역시 동구 동대구로 550",
				BigDecimal.valueOf(35.8797), BigDecimal.valueOf(128.6286), HubType.MAIN, hubId));

		// when & then
		mockMvc.perform(get("/internal/v1/hubs/{hubId}", hubId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.name").value("대구광역시 센터"));
	}

	@Test
	@DisplayName("전체 허브 ID 조회도 토큰 없이 열려 있다")
	void internalHubIdsApiIsOpenWithoutToken() throws Exception {
		// given: permitAll 은 엔드포인트가 아니라 /internal/v1/** 경로 묶음에 걸려 있다.
		// 나중에 이 경로에만 좁은 matcher 가 붙으면 여기서 401·403 으로 드러난다.
		when(hubQueryService.getHubIds()).thenReturn(new HubIdsResult(List.of(UUID.randomUUID())));

		// when & then
		mockMvc.perform(get("/internal/v1/hubs/ids"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.hubIds", hasSize(1)));
	}

	@Test
	@DisplayName("유효한 토큰의 sub 클레임이 callerId 로 서비스에 전달된다")
	void passesTokenSubjectAsCallerId() throws Exception {
		// given: created_by·deleted_by 가 이 값으로 채워지므로 잘못 전달되면 감사 기록이 어긋난다
		UUID userId = UUID.randomUUID();
		String accessToken = token(userId, Role.MASTER, secret, Instant.now().plus(1, ChronoUnit.HOURS));

		UUID hubId = UUID.randomUUID();
		when(hubCommandService.create(any(), any())).thenReturn(new HubCreateResult(
				hubId, "경기 남부 센터", "경기도 이천시 부발읍 경충대로 2091",
				BigDecimal.valueOf(37.272), BigDecimal.valueOf(127.435),
				HubType.MAIN, hubId, "경기 남부 센터", Instant.now(), Instant.now()));

		// when
		mockMvc.perform(post("/api/v1/hubs")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(CREATE_BODY))
				.andExpect(status().isCreated());

		// then
		ArgumentCaptor<UUID> callerIdCaptor = ArgumentCaptor.forClass(UUID.class);
		verify(hubCommandService).create(callerIdCaptor.capture(), any(HubCreateCommand.class));

		assertThat(callerIdCaptor.getValue()).isEqualTo(userId);
	}

	private String token(UUID userId, Role role, String signingSecret, Instant expiration) {
		SecretKey key = Keys.hmacShaKeyFor(signingSecret.getBytes(StandardCharsets.UTF_8));

		return Jwts.builder()
				.subject(userId.toString())
				.claim("role", role.name())
				.issuedAt(Date.from(expiration.minus(1, ChronoUnit.HOURS)))
				.expiration(Date.from(expiration))
				.signWith(key)
				.compact();
	}
}
