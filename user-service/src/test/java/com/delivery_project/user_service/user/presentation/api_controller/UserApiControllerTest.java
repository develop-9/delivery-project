package com.delivery_project.user_service.user.presentation.api_controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Transactional;

import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.repository.RefreshTokenRepository;
import com.delivery_project.user_service.user.domain.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserApiControllerTest {

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

	@Autowired
	private ObjectMapper objectMapper;

	private final List<UUID> loggedInUserIds = new ArrayList<>();

	@AfterEach
	void cleanUpRefreshTokens() {
		loggedInUserIds.forEach(refreshTokenRepository::deleteByUserId);
	}

	@Test
	void MASTER는_승인대기_목록을_조회할_수_있다() {
		// given
		String masterToken = signupApprovedUserAndLogin("master1", "U7000000001", Role.MASTER, null, null);
		signupUser("pendingu1", "U7000000002", Role.COMPANY_MANAGER, null, UUID.randomUUID());

		// when & then
		assertThat(mvc.get().uri("/api/v1/users/pending")
				.header("Authorization", "Bearer " + masterToken))
				.hasStatus(200)
				.bodyJson()
				.extractingPath("$.data.totalElements").isEqualTo(1);
	}

	@Test
	void HUB_MANAGER는_본인_담당_허브의_승인대기자만_조회한다() {
		// given
		UUID myHubId = UUID.randomUUID();
		UUID otherHubId = UUID.randomUUID();
		String hubManagerToken = signupApprovedUserAndLogin("hubmgr2", "U7000000003", Role.HUB_MANAGER, myHubId, null);
		signupUser("pendingu2", "U7000000004", Role.DELIVERY_MANAGER, myHubId, null);
		signupUser("pendingu3", "U7000000005", Role.DELIVERY_MANAGER, otherHubId, null);

		// when & then
		assertThat(mvc.get().uri("/api/v1/users/pending")
				.header("Authorization", "Bearer " + hubManagerToken))
				.hasStatus(200)
				.bodyJson()
				.extractingPath("$.data.totalElements").isEqualTo(1);
	}

	@Test
	void COMPANY_MANAGER는_승인대기_목록_조회시_403을_반환한다() {
		// given
		String companyManagerToken = signupApprovedUserAndLogin("company2", "U7000000006", Role.COMPANY_MANAGER, null, UUID.randomUUID());

		// when & then
		assertThat(mvc.get().uri("/api/v1/users/pending")
				.header("Authorization", "Bearer " + companyManagerToken))
				.hasStatus(403)
				.bodyJson()
				.extractingPath("$.error.errorCode").isEqualTo("READ_USER_FORBIDDEN");
	}

	@Test
	void MASTER가_승인하면_200과_APPROVED_상태를_반환한다() {
		// given
		String masterToken = signupApprovedUserAndLogin("master2", "U7000000007", Role.MASTER, null, null);
		UUID targetId = signupUser("pendingu4", "U7000000008", Role.COMPANY_MANAGER, null, UUID.randomUUID());

		// when & then
		assertThat(mvc.patch().uri("/api/v1/users/{userId}/approve", targetId)
				.header("Authorization", "Bearer " + masterToken))
				.hasStatus(200)
				.bodyJson()
				.extractingPath("$.data.approvalStatus").isEqualTo("APPROVED");
	}

	@Test
	void MASTER가_거절하면_200과_REJECTED_상태를_반환한다() {
		// given
		String masterToken = signupApprovedUserAndLogin("master3", "U7000000009", Role.MASTER, null, null);
		UUID targetId = signupUser("pendingu5", "U7000000010", Role.COMPANY_MANAGER, null, UUID.randomUUID());

		// when & then
		assertThat(mvc.patch().uri("/api/v1/users/{userId}/reject", targetId)
				.header("Authorization", "Bearer " + masterToken))
				.hasStatus(200)
				.bodyJson()
				.extractingPath("$.data.approvalStatus").isEqualTo("REJECTED");
	}

	@Test
	void 이미_처리된_사용자를_다시_승인하려하면_409를_반환한다() {
		// given
		String masterToken = signupApprovedUserAndLogin("master4", "U7000000011", Role.MASTER, null, null);
		UUID targetId = signupUser("pendingu6", "U7000000012", Role.COMPANY_MANAGER, null, UUID.randomUUID());
		mvc.patch().uri("/api/v1/users/{userId}/approve", targetId)
				.header("Authorization", "Bearer " + masterToken)
				.exchange();

		// when & then
		assertThat(mvc.patch().uri("/api/v1/users/{userId}/approve", targetId)
				.header("Authorization", "Bearer " + masterToken))
				.hasStatus(409)
				.bodyJson()
				.extractingPath("$.error.errorCode").isEqualTo("USER_ALREADY_PROCESSED");
	}

	@Test
	void 존재하지_않는_사용자를_승인하려하면_404를_반환한다() {
		// given
		String masterToken = signupApprovedUserAndLogin("master5", "U7000000013", Role.MASTER, null, null);

		// when & then
		assertThat(mvc.patch().uri("/api/v1/users/{userId}/approve", UUID.randomUUID())
				.header("Authorization", "Bearer " + masterToken))
				.hasStatus(404)
				.bodyJson()
				.extractingPath("$.error.errorCode").isEqualTo("USER_NOT_FOUND");
	}

	@Test
	void Authorization_헤더가_없으면_승인시_401을_반환한다() {
		// given
		UUID targetId = signupUser("pendingu7", "U7000000014", Role.COMPANY_MANAGER, null, UUID.randomUUID());

		// when & then
		assertThat(mvc.patch().uri("/api/v1/users/{userId}/approve", targetId))
				.hasStatus(401)
				.bodyJson()
				.extractingPath("$.error.errorCode").isEqualTo("AUTH_TOKEN_INVALID");
	}

	private UUID signupUser(String username, String slackId, Role role, UUID hubId, UUID companyId) {
		String hubField = hubId != null ? "\"hubId\": \"" + hubId + "\"," : "";
		String companyField = companyId != null ? "\"companyId\": \"" + companyId + "\"," : "";
		String body = """
				{
				  "username": "%s",
				  "password": "Abcd1234!",
				  "name": "테스트유저",
				  "slackId": "%s",
				  "role": "%s",
				  %s
				  %s
				  "dummy": null
				}
				""".formatted(username, slackId, role, hubField, companyField);
		mvc.post().uri("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body).exchange();
		return userRepository.findByUsername(username).orElseThrow().getId();
	}

	private String signupApprovedUserAndLogin(String username, String slackId, Role role, UUID hubId, UUID companyId) {
		UUID userId = signupUser(username, slackId, role, hubId, companyId);
		loggedInUserIds.add(userId);

		entityManager.flush();
		entityManager.clear();
		jdbcTemplate.update("UPDATE p_users SET approval_status = 'APPROVED' WHERE username = ?", username);

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
			return data.get("accessToken").asText();
		} catch (Exception e) {
			throw new IllegalStateException("로그인 응답 파싱 실패", e);
		}
	}
}
