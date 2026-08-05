package com.delivery_project.user_service.user.application.command_service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.user.application.result.UserApproveResult;
import com.delivery_project.user_service.user.application.result.UserRejectResult;
import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.entity.User;
import com.delivery_project.user_service.user.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserCommandService {

	private final UserRepository userRepository;

	public UserApproveResult approve(UUID callerId, UUID targetUserId) {
		User caller = resolveCaller(callerId);
		User target = userRepository.findById(targetUserId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		validatePermission(caller, target, ErrorCode.APPROVE_USER_FORBIDDEN);

		if (target.getApprovalStatus() != ApprovalStatus.PENDING) {
			throw new BusinessException(ErrorCode.USER_ALREADY_PROCESSED);
		}

		target.approve(caller.getId());
		log.info("[User] 회원가입 승인 완료 targetUserId={} approvedBy={}", targetUserId, caller.getId());

		return UserApproveResult.from(target);
	}

	public UserRejectResult reject(UUID callerId, UUID targetUserId) {
		User caller = resolveCaller(callerId);
		User target = userRepository.findById(targetUserId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		validatePermission(caller, target, ErrorCode.REJECT_USER_FORBIDDEN);

		if (target.getApprovalStatus() != ApprovalStatus.PENDING) {
			throw new BusinessException(ErrorCode.USER_ALREADY_PROCESSED);
		}

		target.reject();
		log.info("[User] 회원가입 거절 완료 targetUserId={} rejectedBy={}", targetUserId, caller.getId());

		return UserRejectResult.from(target);
	}

	private User resolveCaller(UUID callerId) {
		return userRepository.findById(callerId)
				.orElseThrow(() -> new BusinessException(ErrorCode.AUTH_TOKEN_INVALID));
	}

	private void validatePermission(User caller, User target, ErrorCode forbiddenCode) {
		if (caller.isMaster()) {
			return;
		}
		if (caller.getRole() == Role.HUB_MANAGER) {
			if (!caller.isHubManagerOf(target.getHubId())) {
				throw new BusinessException(ErrorCode.HUB_PERMISSION_DENIED);
			}
			return;
		}
		throw new BusinessException(forbiddenCode);
	}
}
