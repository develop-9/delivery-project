package com.delivery_project.user_service.user.application.support;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.User;
import com.delivery_project.user_service.user.domain.repository.UserCommandRepository;

import lombok.RequiredArgsConstructor;

/**
 * @AuthenticationPrincipal로 주입된 callerId(UUID)를 User 도메인 객체로 변환한다.
 * UserCommandService/UserQueryService에서 각자 구현하던 동일한 조회 로직을 통합.
 */
@Component
@RequiredArgsConstructor
public class CallerResolver {

	private final UserCommandRepository userCommandRepository;

	/**
	 * 로그인 자체가 APPROVED가 아니면 막히므로(AuthCommandService.login/refresh), 지금 이
	 * 검증에 걸리는 경로는 없다 — approve()/reject() 둘 다 PENDING 상태에서만 동작해서 이미
	 * APPROVED된 사용자를 다시 PENDING/REJECTED로 되돌리는 기능 자체가 아직 없기 때문이다.
	 * 다만 이후 "정지" 같은 기능이 추가되면, 이미 발급된 JWT가 살아있는 동안 그 사용자가
	 * 계속 정상 행위자로 취급되는 걸 막을 안전장치로 미리 채워둔다.
	 */
	public User resolve(UUID callerId) {
		User user = userCommandRepository.findById(callerId)
				.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_TOKEN_INVALID));

		if (user.getApprovalStatus() != ApprovalStatus.APPROVED) {
			throw new BusinessException(ErrorCode.USER_NOT_APPROVED);
		}

		return user;
	}
}
