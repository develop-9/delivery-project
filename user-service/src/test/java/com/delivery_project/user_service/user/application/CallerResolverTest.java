package com.delivery_project.user_service.user.application;

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
import com.delivery_project.user_service.user.domain.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class CallerResolverTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private CallerResolver callerResolver;

	@Test
	void 존재하는_callerId면_User를_반환한다() {
		// given
		User user = User.builder()
				.username("kim123")
				.password("encoded-password")
				.name("김철수")
				.slackId("U0123456789")
				.role(Role.MASTER)
				.build();
		UUID callerId = UUID.randomUUID();
		ReflectionTestUtils.setField(user, "id", callerId);
		when(userRepository.findById(callerId)).thenReturn(Optional.of(user));

		// when
		User result = callerResolver.resolve(callerId);

		// then
		assertThat(result).isEqualTo(user);
	}

	@Test
	void 존재하지_않는_callerId면_AUTH_TOKEN_INVALID_예외가_발생한다() {
		// given
		UUID callerId = UUID.randomUUID();
		when(userRepository.findById(callerId)).thenReturn(Optional.empty());

		// when & then
		assertThatThrownBy(() -> callerResolver.resolve(callerId))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.AUTH_TOKEN_INVALID);
	}
}
