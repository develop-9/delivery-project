package com.delivery_project.user_service.user.application.command_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.user.application.command.UserSignupCommand;
import com.delivery_project.user_service.user.application.result.UserSignupResult;
import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.entity.User;
import com.delivery_project.user_service.user.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthCommandServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private AuthCommandService authCommandService;

	@Test
	void 정상_회원가입시_비밀번호가_암호화되어_저장되고_PENDING_상태로_결과가_반환된다() {
		// given
		UserSignupCommand command = new UserSignupCommand(
				"kim123", "Abcd1234!", "김철수", "U0123456789",
				Role.COMPANY_MANAGER, null, UUID.randomUUID());
		User saved = createUser(command);

		when(userRepository.existsByUsername("kim123")).thenReturn(false);
		when(userRepository.existsBySlackId("U0123456789")).thenReturn(false);
		when(passwordEncoder.encode("Abcd1234!")).thenReturn("encoded-password");
		when(userRepository.save(any(User.class))).thenReturn(saved);

		// when
		UserSignupResult result = authCommandService.signup(command);

		// then
		assertThat(result.userId()).isEqualTo(saved.getId());
		assertThat(result.approvalStatus()).isEqualTo(ApprovalStatus.PENDING);
	}

	@Test
	void username이_중복이면_USER_DUPLICATE_USERNAME_예외가_발생한다() {
		// given
		UserSignupCommand command = new UserSignupCommand(
				"kim123", "Abcd1234!", "김철수", "U0123456789",
				Role.COMPANY_MANAGER, null, UUID.randomUUID());

		when(userRepository.existsByUsername("kim123")).thenReturn(true);

		// when & then
		assertThatThrownBy(() -> authCommandService.signup(command))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_DUPLICATE_USERNAME);
	}

	@Test
	void slackId가_중복이면_USER_DUPLICATE_SLACK_ID_예외가_발생한다() {
		// given
		UserSignupCommand command = new UserSignupCommand(
				"kim123", "Abcd1234!", "김철수", "U0123456789",
				Role.COMPANY_MANAGER, null, UUID.randomUUID());

		when(userRepository.existsByUsername("kim123")).thenReturn(false);
		when(userRepository.existsBySlackId("U0123456789")).thenReturn(true);

		// when & then
		assertThatThrownBy(() -> authCommandService.signup(command))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_DUPLICATE_SLACK_ID);
	}

	@Test
	void HUB_MANAGER인데_hubId가_없으면_INVALID_INPUT_VALUE_예외가_발생한다() {
		// given
		UserSignupCommand command = new UserSignupCommand(
				"kim123", "Abcd1234!", "김철수", "U0123456789",
				Role.HUB_MANAGER, null, null);

		// when & then
		assertThatThrownBy(() -> authCommandService.signup(command))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
	}

	@Test
	void COMPANY_MANAGER인데_companyId가_없으면_INVALID_INPUT_VALUE_예외가_발생한다() {
		// given
		UserSignupCommand command = new UserSignupCommand(
				"kim123", "Abcd1234!", "김철수", "U0123456789",
				Role.COMPANY_MANAGER, null, null);

		// when & then
		assertThatThrownBy(() -> authCommandService.signup(command))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.INVALID_INPUT_VALUE);
	}

	@Test
	void MASTER는_hubId_companyId_없이도_회원가입할_수_있다() {
		// given
		UserSignupCommand command = new UserSignupCommand(
				"master1", "Abcd1234!", "관리자", "U0000000000",
				Role.MASTER, null, null);
		User saved = createUser(command);

		when(userRepository.existsByUsername("master1")).thenReturn(false);
		when(userRepository.existsBySlackId("U0000000000")).thenReturn(false);
		when(passwordEncoder.encode("Abcd1234!")).thenReturn("encoded-password");
		when(userRepository.save(any(User.class))).thenReturn(saved);

		// when
		UserSignupResult result = authCommandService.signup(command);

		// then
		assertThat(result.userId()).isEqualTo(saved.getId());
	}

	private User createUser(UserSignupCommand command) {
		return User.builder()
				.username(command.username())
				.password("encoded-password")
				.name(command.name())
				.slackId(command.slackId())
				.role(command.role())
				.hubId(command.hubId())
				.companyId(command.companyId())
				.build();
	}
}
