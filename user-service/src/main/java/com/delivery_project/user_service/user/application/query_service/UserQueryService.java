package com.delivery_project.user_service.user.application.query_service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.user.application.CallerResolver;
import com.delivery_project.user_service.user.application.result.UserPendingResult;
import com.delivery_project.user_service.user.domain.entity.User;
import com.delivery_project.user_service.user.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

	private final UserRepository userRepository;
	private final CallerResolver callerResolver;

	public Page<UserPendingResult> getPendingUsers(UUID callerId, UUID hubIdParam, Pageable pageable) {
		User caller = callerResolver.resolve(callerId);

		Page<User> pendingUsers = switch (caller.getRole()) {
			case MASTER -> hubIdParam != null
					? userRepository.findPendingByHub(hubIdParam, pageable)
					: userRepository.findAllPending(pageable);
			case HUB_MANAGER -> userRepository.findPendingByHub(caller.getHubId(), pageable);
			case COMPANY_MANAGER, DELIVERY_MANAGER -> throw new BusinessException(ErrorCode.READ_USER_FORBIDDEN);
		};

		return pendingUsers.map(UserPendingResult::from);
	}
}
