package com.delivery_project.user_service.user.presentation.api_controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Transactional;

import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthApiControllerTest {

	@Autowired
	private MockMvcTester mvc;

	@Autowired
	private UserRepository userRepository;

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
}
