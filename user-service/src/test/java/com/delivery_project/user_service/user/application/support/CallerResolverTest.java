package com.delivery_project.user_service.user.application.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.entity.User;
import com.delivery_project.user_service.user.domain.repository.UserCommandRepository;

@ExtendWith(MockitoExtension.class)
class CallerResolverTest {

	@Mock
	private UserCommandRepository userCommandRepository;

	@InjectMocks
	private CallerResolver callerResolver;

	@Test
	void 존재하고_APPROVED_상태인_callerId면_User를_반환한다() {
		// given
		User user = createUser();
		user.approve(UUID.randomUUID());
		UUID callerId = UUID.randomUUID();
		ReflectionTestUtils.setField(user, "id", callerId);
		when(userCommandRepository.findById(callerId)).thenReturn(Optional.of(user));

		// when
		User result = callerResolver.resolve(callerId);

		// then
		assertThat(result).isEqualTo(user);
	}

	@Test
	void 존재하지_않는_callerId면_AUTH_TOKEN_INVALID_예외가_발생한다() {
		// given
		UUID callerId = UUID.randomUUID();
		when(userCommandRepository.findById(callerId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> callerResolver.resolve(callerId))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
	}

	@Test
	void PENDING_상태의_callerId면_USER_NOT_APPROVED_예외가_발생한다() {
		// given
		User user = createUser();
		UUID callerId = UUID.randomUUID();
		ReflectionTestUtils.setField(user, "id", callerId);
		when(userCommandRepository.findById(callerId)).thenReturn(Optional.of(user));

		// when & then
		assertThatThrownBy(() -> callerResolver.resolve(callerId))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_NOT_APPROVED);
	}

	@Test
	void REJECTED_상태의_callerId면_USER_NOT_APPROVED_예외가_발생한다() {
		// given
		User user = createUser();
		user.reject();
		UUID callerId = UUID.randomUUID();
		ReflectionTestUtils.setField(user, "id", callerId);
		when(userCommandRepository.findById(callerId)).thenReturn(Optional.of(user));

		// when & then
		assertThatThrownBy(() -> callerResolver.resolve(callerId))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.USER_NOT_APPROVED);
	}

	private User createUser() {
		return User.builder()
				.username("kim123")
				.password("encoded-password")
				.name("김철수")
				.slackId("U0123456789")
				.role(Role.MASTER)
				.build();
	}
}
