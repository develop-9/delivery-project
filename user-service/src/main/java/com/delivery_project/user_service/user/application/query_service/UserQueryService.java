package com.delivery_project.user_service.user.application.query_service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.user.application.support.CallerResolver;
import com.delivery_project.user_service.user.application.result.UserDetailResult;
import com.delivery_project.user_service.user.application.result.UserListResult;
import com.delivery_project.user_service.user.application.result.UserPendingResult;
import com.delivery_project.user_service.user.domain.entity.User;
import com.delivery_project.user_service.user.domain.repository.UserQueryRepository;
import com.delivery_project.user_service.user.domain.repository.UserSearchCondition;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

	private final UserQueryRepository userQueryRepository;
	private final CallerResolver callerResolver;

	public UserDetailResult getMe(UUID callerId) {
		User caller = callerResolver.resolve(callerId);
		return UserDetailResult.from(caller);
	}

	public Page<UserListResult> list(UUID callerId, UserSearchCondition condition, Pageable pageable) {
		User caller = callerResolver.resolve(callerId);
		if (!caller.isMaster()) {
			throw new BusinessException(ErrorCode.READ_USER_FORBIDDEN, "사용자 목록 조회 권한이 없습니다.");
		}

		return userQueryRepository.search(condition, pageable).map(UserListResult::from);
	}

	public Page<UserPendingResult> getPendingUsers(UUID callerId, UUID hubIdParam, Pageable pageable) {
		User caller = callerResolver.resolve(callerId);

		Page<User> pendingUsers = switch (caller.getRole()) {
			case MASTER -> hubIdParam != null
					? userQueryRepository.findPendingByHub(hubIdParam, pageable)
					: userQueryRepository.findAllPending(pageable);
			case HUB_MANAGER -> userQueryRepository.findPendingByHub(caller.getHubId(), pageable);
			case COMPANY_MANAGER, DELIVERY_MANAGER ->
					throw new BusinessException(ErrorCode.READ_USER_FORBIDDEN, "승인 대기자 조회 권한이 없습니다.");
		};

		return pendingUsers.map(UserPendingResult::from);
	}
}
