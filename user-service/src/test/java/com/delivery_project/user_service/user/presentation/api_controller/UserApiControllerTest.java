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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;
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
 * @MockitoBean으로 대체한다 — signupUser() 헬퍼가 내부적으로 실제 회원가입 API를 호출하는데,
 * 기본 동작(예외 없이 통과)이 "허브/업체가 존재한다"는 뜻이라 이 파일의 나머지 테스트(정지/승인/
 * 거절 등)에는 영향이 없다.
 *
 * signup()은 Hub/Company Feign 호출을 트랜잭션 밖에서 하도록 NOT_SUPPORTED로 되어 있어서,
 * signupUser() 헬퍼가 만든 행은 이 클래스의 @Transactional과 무관한 별도 트랜잭션에서
 * 커밋된다 — 즉 테스트가 끝나도 롤백되지 않고 실제로 남는다. cleanUpUsers()가 매 테스트 뒤
 * p_users를 직접 비운다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserApiControllerTest {

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

	private final List<UUID> loggedInUserIds = new ArrayList<>();

	@AfterEach
	void cleanUpRefreshTokens() {
		loggedInUserIds.forEach(refreshTokenRepository::deleteByUserId);
	}

	/**
	 * signup()이 커밋한 행은 이 클래스의 @Transactional 롤백 범위 밖이라 여기서 직접 지운다.
	 * TestTransaction.end()로 이 테스트가 남긴 진행 중인 트랜잭션을 먼저 정리한 뒤(원래도
	 * 롤백될 것들이라 먼저 끝내도 결과는 같다) DELETE를 새 커넥션에서 즉시 커밋되게 실행한다 —
	 * 그래야 signup()이 이미 커밋해버린 행도 실제로 지워진다.
	 */
	@AfterEach
	void cleanUpUsers() {
		if (TestTransaction.isActive()) {
			TestTransaction.end();
		}
		jdbcTemplate.update("DELETE FROM p_users");
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
	void 허용되지_않은_size로_승인대기_목록을_조회하면_기본값_10으로_보정된다() {
		// given
		String masterToken = signupApprovedUserAndLogin("master6", "U7000000015", Role.MASTER, null, null);

		// when & then
		assertThat(mvc.get().uri("/api/v1/users/pending?size=25")
				.header("Authorization", "Bearer " + masterToken))
				.hasStatus(200)
				.bodyJson()
				.extractingPath("$.data.size").isEqualTo(10);
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

	@Test
	void MASTER가_정지하면_200과_SUSPENDED_상태를_반환한다() {
		// given
		String masterToken = signupApprovedUserAndLogin("master7", "U7000000016", Role.MASTER, null, null);
		signupApprovedUserAndLogin("target7", "U7000000017", Role.COMPANY_MANAGER, null, UUID.randomUUID());
		UUID targetId = userCommandRepository.findByUsername("target7").orElseThrow().getId();

		// when & then
		assertThat(mvc.patch().uri("/api/v1/users/{userId}/suspend", targetId)
				.header("Authorization", "Bearer " + masterToken))
				.hasStatus(200)
				.bodyJson()
				.extractingPath("$.data.approvalStatus").isEqualTo("SUSPENDED");
	}

	@Test
	void 정지된_계정의_기존_토큰으로는_이후_요청이_403을_반환한다() {
		// given: 정지 이전에 발급받은 토큰을 그대로 재사용 — CallerResolver가 승인 상태를 다시 검증하는지 확인
		String masterToken = signupApprovedUserAndLogin("master8", "U7000000018", Role.MASTER, null, null);
		String targetToken = signupApprovedUserAndLogin("target8", "U7000000019", Role.COMPANY_MANAGER, null, UUID.randomUUID());
		UUID targetId = userCommandRepository.findByUsername("target8").orElseThrow().getId();
		mvc.patch().uri("/api/v1/users/{userId}/suspend", targetId)
				.header("Authorization", "Bearer " + masterToken)
				.exchange();

		// when & then
		assertThat(mvc.get().uri("/api/v1/users/me")
				.header("Authorization", "Bearer " + targetToken))
				.hasStatus(403)
				.bodyJson()
				.extractingPath("$.error.errorCode").isEqualTo("USER_NOT_APPROVED");
	}

	@Test
	void MASTER가_아니면_정지시_403을_반환한다() {
		// given
		String companyManagerToken = signupApprovedUserAndLogin("company9", "U7000000020", Role.COMPANY_MANAGER, null, UUID.randomUUID());
		UUID targetId = signupUser("target9", "U7000000021", Role.COMPANY_MANAGER, null, UUID.randomUUID());

		// when & then
		assertThat(mvc.patch().uri("/api/v1/users/{userId}/suspend", targetId)
				.header("Authorization", "Bearer " + companyManagerToken))
				.hasStatus(403)
				.bodyJson()
				.extractingPath("$.error.errorCode").isEqualTo("SUSPEND_USER_FORBIDDEN");
	}

	@Test
	void PENDING_사용자를_정지하려하면_409를_반환한다() {
		// given
		String masterToken = signupApprovedUserAndLogin("master10", "U7000000022", Role.MASTER, null, null);
		UUID targetId = signupUser("target10", "U7000000023", Role.COMPANY_MANAGER, null, UUID.randomUUID());

		// when & then
		assertThat(mvc.patch().uri("/api/v1/users/{userId}/suspend", targetId)
				.header("Authorization", "Bearer " + masterToken))
				.hasStatus(409)
				.bodyJson()
				.extractingPath("$.error.errorCode").isEqualTo("USER_NOT_SUSPENDABLE");
	}

	@Test
	void 존재하지_않는_사용자를_정지하려하면_404를_반환한다() {
		// given
		String masterToken = signupApprovedUserAndLogin("master11", "U7000000024", Role.MASTER, null, null);

		// when & then
		assertThat(mvc.patch().uri("/api/v1/users/{userId}/suspend", UUID.randomUUID())
				.header("Authorization", "Bearer " + masterToken))
				.hasStatus(404)
				.bodyJson()
				.extractingPath("$.error.errorCode").isEqualTo("USER_NOT_FOUND");
	}

	@Test
	void MASTER가_정지_해제하면_200과_APPROVED_상태를_반환한다() {
		// given
		String masterToken = signupApprovedUserAndLogin("master12", "U7000000025", Role.MASTER, null, null);
		signupApprovedUserAndLogin("target12", "U7000000026", Role.COMPANY_MANAGER, null, UUID.randomUUID());
		UUID targetId = userCommandRepository.findByUsername("target12").orElseThrow().getId();
		mvc.patch().uri("/api/v1/users/{userId}/suspend", targetId)
				.header("Authorization", "Bearer " + masterToken)
				.exchange();

		// when & then
		assertThat(mvc.patch().uri("/api/v1/users/{userId}/reinstate", targetId)
				.header("Authorization", "Bearer " + masterToken))
				.hasStatus(200)
				.bodyJson()
				.extractingPath("$.data.approvalStatus").isEqualTo("APPROVED");
	}

	@Test
	void 정지되지_않은_사용자를_정지_해제하려하면_409를_반환한다() {
		// given
		String masterToken = signupApprovedUserAndLogin("master13", "U7000000027", Role.MASTER, null, null);
		signupApprovedUserAndLogin("target13", "U7000000028", Role.COMPANY_MANAGER, null, UUID.randomUUID());
		UUID targetId = userCommandRepository.findByUsername("target13").orElseThrow().getId();

		// when & then
		assertThat(mvc.patch().uri("/api/v1/users/{userId}/reinstate", targetId)
				.header("Authorization", "Bearer " + masterToken))
				.hasStatus(409)
				.bodyJson()
				.extractingPath("$.error.errorCode").isEqualTo("USER_NOT_SUSPENDED");
	}

	@Test
	void Authorization_헤더가_없으면_정지시_401을_반환한다() {
		// given
		UUID targetId = signupUser("target14", "U7000000029", Role.COMPANY_MANAGER, null, UUID.randomUUID());

		// when & then
		assertThat(mvc.patch().uri("/api/v1/users/{userId}/suspend", targetId))
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
		return userCommandRepository.findByUsername(username).orElseThrow().getId();
	}

	private String signupApprovedUserAndLogin(String username, String slackId, Role role, UUID hubId, UUID companyId) {
		UUID userId = signupUser(username, slackId, role, hubId, companyId);
		loggedInUserIds.add(userId);

		// 같은 테스트 안에서 이 헬퍼를 두 번째로 부르는 경우(master, target 각각)엔 첫 호출이
		// 이미 트랜잭션을 끝내둬서 flush()가 트랜잭션을 요구하는 예외를 던진다 — 활성 트랜잭션이
		// 있을 때만 의미가 있으므로(signup()이 이미 별도 트랜잭션에서 실제로 커밋하기 때문에
		// 원래도 필수는 아니었다) 있을 때만 실행한다.
		if (TestTransaction.isActive()) {
			entityManager.flush();
			entityManager.clear();
		}
		jdbcTemplate.update("UPDATE p_users SET approval_status = 'APPROVED' WHERE username = ?", username);

		// 이 UPDATE를 커밋해야 한다 — suspend()/reinstate()/delete()가 NOT_SUPPORTED라 같은
		// 유저를 대상으로 나중에 호출되면 별도 트랜잭션에서 이 행을 다시 건드리는데, 이 UPDATE가
		// 이 테스트의 트랜잭션 안에 미커밋 상태로 남아 있으면 그 행에 락이 걸린 채라 데드락에
		// 빠진다(AuthApiControllerTest의 재가입 테스트에서 실제로 겪은 것과 같은 종류).
		//
		// 커밋 뒤 트랜잭션을 다시 시작하지 않는다 — 재시작하면 이 테스트 메서드가 그 뒤로도 계속
		// 같은 영속성 컨텍스트를 쓰게 되는데, 이미 이 안에서 조회했던 엔티티(예: findByUsername으로
		// targetId를 구할 때)가 1차 캐시에 남아있어서, suspend()/reinstate()가 별도 트랜잭션에서
		// 실제로 커밋한 변경을 이후 조회(예: /users/me)가 못 보고 캐시된 옛 값을 반환하는 문제가
		// 실제로 있었다. 트랜잭션 없이 두면 이후 호출들은 운영과 동일하게 각자 새 트랜잭션/영속성
		// 컨텍스트로 커밋된 최신 값을 그대로 읽는다.
		if (TestTransaction.isActive()) {
			TestTransaction.flagForCommit();
			TestTransaction.end();
		}

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
