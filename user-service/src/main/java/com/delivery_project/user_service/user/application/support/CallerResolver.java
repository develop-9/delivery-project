package com.delivery_project.user_service.user.application.support;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.user.domain.entity.User;
import com.delivery_project.user_service.user.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * @AuthenticationPrincipal로 주입된 callerId(UUID)를 User 도메인 객체로 변환한다.
 * UserCommandService/UserQueryService에서 각자 구현하던 동일한 조회 로직을 통합.
 */
@Component
@RequiredArgsConstructor
public class CallerResolver {

	private final UserRepository userRepository;

	public User resolve(UUID callerId) {
		return userRepository.findById(callerId)
				.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_TOKEN_INVALID));
	}
}
