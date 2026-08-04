package com.delivery_project.user_service.user.presentation.api_controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.repository.RefreshTokenRepository;
import com.delivery_project.user_service.user.domain.repository.UserRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthApiControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@PersistenceContext
	private EntityManager entityManager;

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

		assertThat(userRepository.existsByUsername("kim123")).isTrue();
	}

	@Test
	void username이_중복이면_409를_반환한다() {
		// given
		userRepository.save(
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

		UUID userId = userRepository.findByUsername("loginuser").orElseThrow().getId();

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
}
