package com.delivery_project.user_service.user.application.query_service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.user.application.result.UserPendingResult;
import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.entity.User;
import com.delivery_project.user_service.user.domain.repository.UserRepository;
import com.delivery_project.user_service.user.infrastructure.jwt.CurrentUserResolver;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

	private final UserRepository userRepository;
	private final CurrentUserResolver currentUserResolver;

	public Page<UserPendingResult> getPendingUsers(String authorizationHeader, UUID hubIdParam, Pageable pageable) {
		User caller = currentUserResolver.resolve(authorizationHeader);

		Page<User> pendingUsers = switch (caller.getRole()) {
			case MASTER -> hubIdParam != null
					? userRepository.findByApprovalStatusAndHubId(ApprovalStatus.PENDING, hubIdParam, pageable)
					: userRepository.findByApprovalStatus(ApprovalStatus.PENDING, pageable);
			case HUB_MANAGER -> userRepository.findByApprovalStatusAndHubId(ApprovalStatus.PENDING, caller.getHubId(), pageable);
			case COMPANY_MANAGER, DELIVERY_MANAGER -> throw new BusinessException(ErrorCode.READ_USER_FORBIDDEN);
		};

		return pendingUsers.map(UserPendingResult::from);
	}
}
