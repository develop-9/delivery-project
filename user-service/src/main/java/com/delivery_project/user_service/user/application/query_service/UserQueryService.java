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
import com.delivery_project.user_service.user.application.result.UserPendingResult;
import com.delivery_project.user_service.user.domain.entity.User;
import com.delivery_project.user_service.user.domain.repository.UserQueryRepository;

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

	public Page<UserPendingResult> getPendingUsers(UUID callerId, UUID hubIdParam, Pageable pageable) {
		User caller = callerResolver.resolve(callerId);

		Page<User> pendingUsers = switch (caller.getRole()) {
			case MASTER -> hubIdParam != null
					? userQueryRepository.findPendingByHub(hubIdParam, pageable)
					: userQueryRepository.findAllPending(pageable);
			case HUB_MANAGER -> userQueryRepository.findPendingByHub(caller.getHubId(), pageable);
			case COMPANY_MANAGER, DELIVERY_MANAGER -> throw new BusinessException(ErrorCode.READ_USER_FORBIDDEN);
		};

		return pendingUsers.map(UserPendingResult::from);
	}
}
