package com.delivery_project.user_service.user.infrastructure.jwt;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.user.domain.entity.User;
import com.delivery_project.user_service.user.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * JwtAuthenticationFilter/SecurityContext가 아직 없는 동안, Authorization 헤더의
 * Access Token으로부터 요청자(User)를 직접 조회하기 위한 임시 대체 수단.
 */
@Component
@RequiredArgsConstructor
public class CurrentUserResolver {

	private final JwtProvider jwtProvider;
	private final UserRepository userRepository;

	public User resolve(String authorizationHeader) {
		String token = jwtProvider.resolveToken(authorizationHeader);
		UUID userId = jwtProvider.parse(token).userId();

		return userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_TOKEN_INVALID));
	}
}
