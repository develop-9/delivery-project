package com.delivery_project.user_service.user.presentation.api_controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

import com.delivery_project.user_service.user.application.port.CompanyPort;
import com.delivery_project.user_service.user.application.port.HubPort;
import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.repository.RefreshTokenRepository;
import com.delivery_project.user_service.user.domain.repository.UserCommandRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * HubPort/CompanyPort는 실제 Hub/Company Service를 이 테스트 환경에서 띄우지 않으므로
 * @MockitoBean으로 대체한다 — 기본 동작(예외 없이 통과)이 "허브/업체가 존재한다"는 뜻이라,
 * hubId/companyId 검증 자체를 다루는 게 아닌 나머지 회원가입/로그인/로그아웃 테스트에 영향이 없다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthApiControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private UserCommandRepository userCommandRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private HubPort hubPort;

	@MockitoBean
	private CompanyPort companyPort;

	@Test
	void 회원가입_성공시_201과_PENDING_상태를_반환한다() {
		// given
		String body = """
				{
				  "username": "kim123",
				  "password": "Abcd1234!",
				  "name": "김철수",
				  "slackId": "U0123456789",
				  "role": "COMPANY_MANAGER",
				  "companyId": "%s"
				}
				""".formatted(UUID.randomUUID());

		// when & then
		assertThat(mvc.post().uri("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.hasStatus(201)
				.bodyJson()
				.extractingPath("$.data.approvalStatus").isEqualTo("PENDING");

		assertThat(userCommandRepository.existsByUsername("kim123")).isTrue();
	}

	@Test
	void username이_중복이면_409를_반환한다() {
		// given
		userCommandRepository.save(
				com.delivery_project.user_service.user.domain.entity.User.builder()
						.username("dupuser")
						.password("encoded")
						.name("기존유저")
						.slackId("U9999999999")
						.role(Role.COMPANY_MANAGER)
						.companyId(UUID.randomUUID())
						.build()
		);

		String body = """
				{
				  "username": "dupuser",
				  "password": "Abcd1234!",
				  "name": "새유저",
				  "slackId": "U0123456789",
				  "role": "COMPANY_MANAGER",
				  "companyId": "%s"
				}
				""".formatted(UUID.randomUUID());

		// when & then
		assertThat(mvc.post().uri("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.hasStatus(409)
				.bodyJson()
				.extractingPath("$.error.errorCode").isEqualTo("USER_DUPLICATE_USERNAME");
	}

	@Test
	void 필수값이_누락되면_400을_반환한다() {
		// given
		String body = """
				{
				  "username": "",
				  "password": "Abcd1234!",
				  "name": "김철수",
				  "slackId": "U0123456789",
				  "role": "COMPANY_MANAGER",
				  "companyId": "%s"
				}
				""".formatted(UUID.randomUUID());

		// when & then
		assertThat(mvc.post().uri("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.hasStatus(400)
				.bodyJson()
				.extractingPath("$.error.errorCode").isEqualTo("INVALID_INPUT_VALUE");
	}

	@Test
	void HUB_MANAGER인데_hubId가_없으면_400을_반환한다() {
		// given
		String body = """
				{
				  "username": "hubmgr1",
				  "password": "Abcd1234!",
				  "name": "허브담당자",
				  "slackId": "U1111111111",
				  "role": "HUB_MANAGER"
				}
				""";

		// when & then
		assertThat(mvc.post().uri("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.hasStatus(400)
				.bodyJson()
				.extractingPath("$.error.errorCode").isEqualTo("INVALID_INPUT_VALUE");
	}

	@Test
	void 존재하지_않는_hubId로_가입하면_404를_반환한다() {
		// given
		UUID hubId = UUID.randomUUID();
		org.mockito.Mockito.doThrow(new com.delivery_project.user_service.global.exception.BusinessException(
						com.delivery_project.user_service.global.exception.ErrorCode.HUB_NOT_FOUND))
				.when(hubPort).validateExists(hubId);

		String body = """
				{
				  "username": "hubmgr2",
				  "password": "Abcd1234!",
				  "name": "허브담당자2",
				  "slackId": "U1111111112",
				  "role": "HUB_MANAGER",
				  "hubId": "%s"
				}
				""".formatted(hubId);

		// when & then
		assertThat(mvc.post().uri("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.hasStatus(404)
				.bodyJson()
				.extractingPath("$.error.errorCode").isEqualTo("HUB_NOT_FOUND");

		assertThat(userCommandRepository.existsByUsername("hubmgr2")).isFalse();
	}

	@Test
	void 로그인_성공시_토큰을_반환하고_RefreshToken이_Redis에_저장된다() {
		// given
		String signupBody = """
				{
				  "username": "loginuser",
				  "password": "Abcd1234!",
				  "name": "로그인유저",
				  "slackId": "U2222222222",
				  "role": "COMPANY_MANAGER",
				  "companyId": "%s"
				}
				""".formatted(UUID.randomUUID());
		mvc.post().uri("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signupBody).exchange();

		// signup의 INSERT를 먼저 flush로 DB에 반영한 뒤, 캐시된 PENDING 상태 User를 clear로 비워서
		// 아래 JDBC 직접 갱신이 이후 조회에 반영되게 한다 (flush 없이 clear만 하면 미반영 INSERT가 유실됨)
		entityManager.flush();
		entityManager.clear();
		jdbcTemplate.update("UPDATE p_users SET approval_status = 'APPROVED' WHERE username = ?", "loginuser");

		UUID userId = userCommandRepository.findByUsername("loginuser").orElseThrow().getId();

		String loginBody = """
				{
				  "username": "loginuser",
				  "password": "Abcd1234!"
				}
				""";

		try {
			// when & then
			assertThat(mvc.post().uri("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(loginBody))
					.hasStatus(200)
					.bodyJson()
					.extractingPath("$.data.accessToken").isNotNull();

			assertThat(refreshTokenRepository.findByUserId(userId)).isPresent();
		} finally {
			refreshTokenRepository.deleteByUserId(userId);
		}
	}

	@Test
	void 존재하지_않는_username으로_로그인하면_401을_반환한다() {
		// given
		String body = """
				{
				  "username": "no-such-user",
				  "password": "Abcd1234!"
				}
				""";

		// when & then
		assertThat(mvc.post().uri("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.hasStatus(401)
				.bodyJson()
				.extractingPath("$.error.errorCode").isEqualTo("AUTH_INVALID_CREDENTIALS");
	}

	@Test
	void 비밀번호가_틀리면_401을_반환한다() {
		// given
		String signupBody = """
				{
				  "username": "wrongpw1",
				  "password": "Abcd1234!",
				  "name": "테스트유저",
				  "slackId": "U3333333333",
				  "role": "COMPANY_MANAGER",
				  "companyId": "%s"
				}
				""".formatted(UUID.randomUUID());
		mvc.post().uri("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signupBody).exchange();

		String loginBody = """
				{
				  "username": "wrongpw1",
				  "password": "WrongPassword1!"
				}
				""";

		// when & then
		assertThat(mvc.post().uri("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody))
				.hasStatus(401)
				.bodyJson()
				.extractingPath("$.error.errorCode").isEqualTo("AUTH_INVALID_CREDENTIALS");
	}

	@Test
	void 승인되지_않은_사용자는_로그인시_403을_반환한다() {
		// given
		String signupBody = """
				{
				  "username": "pending1",
				  "password": "Abcd1234!",
				  "name": "대기유저",
				  "slackId": "U4444444444",
				  "role": "COMPANY_MANAGER",
				  "companyId": "%s"
				}
				""".formatted(UUID.randomUUID());
		mvc.post().uri("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signupBody).exchange();

		String loginBody = """
				{
				  "username": "pending1",
				  "password": "Abcd1234!"
				}
				""";

		// when & then
		assertThat(mvc.post().uri("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody))
				.hasStatus(403)
				.bodyJson()
				.extractingPath("$.error.errorCode").isEqualTo("USER_NOT_APPROVED");
	}

	@Test
	void 소프트_삭제된_사용자와_같은_username으로_재가입하면_새_계정으로_201을_반환한다() {
		// given: 삭제 API가 아직 없어서 JDBC로 소프트 삭제 상태를 직접 재현
		String signupBody = """
				{
				  "username": "softdel1",
				  "password": "Abcd1234!",
				  "name": "삭제될유저",
				  "slackId": "U8888888881",
				  "role": "COMPANY_MANAGER",
				  "companyId": "%s"
				}
				""".formatted(UUID.randomUUID());
		mvc.post().uri("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signupBody).exchange();

		entityManager.flush();
		entityManager.clear();
		jdbcTemplate.update("UPDATE p_users SET deleted_at = now() WHERE username = ?", "softdel1");

		assertThat(userCommandRepository.existsByUsername("softdel1")).isFalse();

		String reSignupBody = """
				{
				  "username": "softdel1",
				  "password": "Abcd1234!",
				  "name": "재가입시도",
				  "slackId": "U8888888882",
				  "role": "COMPANY_MANAGER",
				  "companyId": "%s"
				}
				""".formatted(UUID.randomUUID());

		// when & then: username/slack_id의 유일성은 삭제되지 않은 행에만 적용되는 부분 유니크
		// 인덱스(UserTableSchemaInitializer)라, 삭제된 사용자의 username 재사용은 더 이상 막히지 않는다.
		assertThat(mvc.post().uri("/api/v1/auth/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(reSignupBody))
				.hasStatus(201)
				.bodyJson()
				.extractingPath("$.data.approvalStatus").isEqualTo("PENDING");

		assertThat(userCommandRepository.existsByUsername("softdel1")).isTrue();
	}

	@Test
	void 정상_토큰_재발급시_새_토큰을_반환하고_이전_RefreshToken은_더_이상_쓸_수_없다() {
		// given
		Tokens tokens = signupApprovedUserAndLogin("refresh1", "U5555555555");
		String refreshBody = """
				{
				  "refreshToken": "%s"
				}
				""".formatted(tokens.refreshToken());

		try {
			// when & then
			assertThat(mvc.post().uri("/api/v1/auth/refresh")
					.contentType(MediaType.APPLICATION_JSON)
					.content(refreshBody))
					.hasStatus(200)
					.bodyJson()
					.extractingPath("$.data.refreshToken").isNotEqualTo(tokens.refreshToken());

			// 이전 refreshToken 재사용 시도는 실패해야 한다
			assertThat(mvc.post().uri("/api/v1/auth/refresh")
					.contentType(MediaType.APPLICATION_JSON)
					.content(refreshBody))
					.hasStatus(401)
					.bodyJson()
					.extractingPath("$.error.errorCode").isEqualTo("AUTH_TOKEN_EXPIRED");
		} finally {
			refreshTokenRepository.deleteByUserId(tokens.userId());
		}
	}

	@Test
	void 형식이_잘못된_RefreshToken은_401을_반환한다() {
		// given
		String body = """
				{
				  "refreshToken": "not-a-valid-jwt"
				}
				""";

		// when & then
		assertThat(mvc.post().uri("/api/v1/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
				.hasStatus(401)
				.bodyJson()
				.extractingPath("$.error.errorCode").isEqualTo("AUTH_TOKEN_INVALID");
	}

	@Test
	void 정상_로그아웃시_204를_반환하고_RefreshToken이_삭제된다() {
		// given
		Tokens tokens = signupApprovedUserAndLogin("logoutuser", "U6666666666");

		// when & then
		assertThat(mvc.post().uri("/api/v1/auth/logout")
				.header("Authorization", "Bearer " + tokens.accessToken()))
				.hasStatus(204);

		assertThat(refreshTokenRepository.findByUserId(tokens.userId())).isEmpty();
	}

	@Test
	void Authorization_헤더가_없으면_로그아웃시_401을_반환한다() {
		// when & then
		assertThat(mvc.post().uri("/api/v1/auth/logout"))
				.hasStatus(401)
				.bodyJson()
				.extractingPath("$.error.errorCode").isEqualTo("AUTH_TOKEN_INVALID");
	}

	private record Tokens(UUID userId, String accessToken, String refreshToken) {
	}

	private Tokens signupApprovedUserAndLogin(String username, String slackId) {
		String signupBody = """
				{
				  "username": "%s",
				  "password": "Abcd1234!",
				  "name": "테스트유저",
				  "slackId": "%s",
				  "role": "COMPANY_MANAGER",
				  "companyId": "%s"
				}
				""".formatted(username, slackId, UUID.randomUUID());
		mvc.post().uri("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON).content(signupBody).exchange();

		entityManager.flush();
		entityManager.clear();
		jdbcTemplate.update("UPDATE p_users SET approval_status = 'APPROVED' WHERE username = ?", username);

		UUID userId = userCommandRepository.findByUsername(username).orElseThrow().getId();

		String loginBody = """
				{
				  "username": "%s",
				  "password": "Abcd1234!"
				}
				""".formatted(username);
		MvcTestResult result = mvc.post().uri("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody)
				.exchange();

		try {
			JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
			return new Tokens(userId, data.get("accessToken").asText(), data.get("refreshToken").asText());
		} catch (Exception e) {
			throw new IllegalStateException("로그인 응답 파싱 실패", e);
		}
	}
}
