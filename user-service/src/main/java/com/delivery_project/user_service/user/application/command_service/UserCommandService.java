package com.delivery_project.user_service.user.application.command_service;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.user.application.command.UserApproveCommand;
import com.delivery_project.user_service.user.application.command.UserDeleteCommand;
import com.delivery_project.user_service.user.application.command.UserRejectCommand;
import com.delivery_project.user_service.user.application.command.UserUpdateMeCommand;
import com.delivery_project.user_service.user.application.support.CallerResolver;
import com.delivery_project.user_service.user.application.result.UserApproveResult;
import com.delivery_project.user_service.user.application.result.UserDeleteResult;
import com.delivery_project.user_service.user.application.result.UserRejectResult;
import com.delivery_project.user_service.user.application.result.UserUpdateMeResult;
import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.entity.User;
import com.delivery_project.user_service.user.domain.repository.RefreshTokenRepository;
import com.delivery_project.user_service.user.domain.repository.UserCommandRepository;
import com.delivery_project.user_service.user.infrastructure.client.delivery.DeliveryManagerClient;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserCommandService {

	private final UserCommandRepository userCommandRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final DeliveryManagerClient deliveryManagerClient;
	private final CallerResolver callerResolver;

	public UserUpdateMeResult updateMe(UUID callerId, UserUpdateMeCommand command) {
		User caller = callerResolver.resolve(callerId);

		if (command.slackId() != null
				&& !command.slackId().equals(caller.getSlackId())
				&& userCommandRepository.existsBySlackId(command.slackId())) {
			throw new BusinessException(ErrorCode.USER_DUPLICATE_SLACK_ID);
		}

		caller.updateProfile(command.name(), command.slackId());

		// existsBySlackId 사전검사와 저장 사이에 레이스가 있을 수 있어, save()로 즉시 flush해서
		// 그 사이 동시 요청으로 인한 UNIQUE 제약 위반을 여기서 구체적인 에러코드로 잡는다
		// (AuthCommandService.saveUser()와 동일한 패턴).
		try {
			userCommandRepository.save(caller);
		} catch (DataIntegrityViolationException e) {
			throw new BusinessException(ErrorCode.USER_DUPLICATE_SLACK_ID);
		}

		log.info("[User] 내 정보 수정 완료 userId={}", caller.getId());

		return UserUpdateMeResult.from(caller);
	}

	public UserApproveResult approve(UUID callerId, UserApproveCommand command) {
		User caller = callerResolver.resolve(callerId);
		User target = userCommandRepository.findById(command.targetUserId())
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		validatePermission(caller, target, ErrorCode.APPROVE_USER_FORBIDDEN);

		if (target.getApprovalStatus() != ApprovalStatus.PENDING) {
			throw new BusinessException(ErrorCode.USER_ALREADY_PROCESSED);
		}

		target.approve(caller.getId());
		log.info("[User] 회원가입 승인 완료 targetUserId={} approvedBy={}", command.targetUserId(), caller.getId());

		return UserApproveResult.from(target);
	}

	public UserRejectResult reject(UUID callerId, UserRejectCommand command) {
		User caller = callerResolver.resolve(callerId);
		User target = userCommandRepository.findById(command.targetUserId())
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		validatePermission(caller, target, ErrorCode.REJECT_USER_FORBIDDEN);

		if (target.getApprovalStatus() != ApprovalStatus.PENDING) {
			throw new BusinessException(ErrorCode.USER_ALREADY_PROCESSED);
		}

		target.reject();
		log.info("[User] 회원가입 거절 완료 targetUserId={} rejectedBy={}", command.targetUserId(), caller.getId());

		return UserRejectResult.from(target);
	}

	public UserDeleteResult delete(UUID callerId, UserDeleteCommand command) {
		User caller = callerResolver.resolve(callerId);
		if (!caller.isMaster()) {
			throw new BusinessException(ErrorCode.DELETE_USER_FORBIDDEN);
		}

		User target = userCommandRepository.findById(command.targetUserId())
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		if (target.getRole() == Role.MASTER
				&& target.getApprovalStatus() == ApprovalStatus.APPROVED
				&& userCommandRepository.countActiveMasters() <= 1) {
			throw new BusinessException(ErrorCode.LAST_MASTER_DELETE_FORBIDDEN);
		}

		// COMPANY_MANAGER는 Company Service에 연동 대상 레코드가 없어 확인 결과 연동 불필요로 확정됨.
		if (target.getRole() == Role.DELIVERY_MANAGER) {
			syncDeleteDeliveryManager(target.getId());
		}

		target.delete(caller.getId());
		refreshTokenRepository.deleteByUserId(target.getId());
		log.info("[User] 사용자 삭제 완료 targetUserId={} deletedBy={}", command.targetUserId(), caller.getId());

		return UserDeleteResult.from(target);
	}

	/**
	 * Delivery Service 연동이 (레코드가 원래 없는 404 제외하고) 실패하면 사용자 삭제 자체를 막는다.
	 * 정리가 안 된 배송담당자 레코드를 Delivery Service 쪽에 활성 상태로 남겨두면, User Service에서는
	 * 이미 삭제되어 로그인도 안 되는 사람에게 라운드로빈으로 새 배송이 배정될 수 있어 "최선 노력"보다
	 * 안전한 실패(차단) 쪽을 택함. 재시도 장치가 없어 한 번 어긋나면 계속 그 상태로 남는 것도 이유.
	 *
	 * TODO: Delivery Service의 내부 삭제 API가 "진행 중인 배송 배정 여부"를 검증하게 되면
	 * (Delivery/DeliveryRoute 구현 후), 그 결과를 여기서 전용 ErrorCode로 구분해 반영해야 한다.
	 */
	private void syncDeleteDeliveryManager(UUID userId) {
		try {
			deliveryManagerClient.deleteByUserId(userId);
		} catch (FeignException.NotFound e) {
			// 배송담당자 레코드가 없거나 이미 삭제됨 — User 삭제를 막을 이유가 아니므로 무시.
			log.info("[User] 연동할 배송담당자 레코드 없음 userId={}", userId);
		} catch (FeignException e) {
			log.warn("[User] Delivery Service 연동 실패 userId={}", userId, e);
			throw new BusinessException(ErrorCode.DELIVERY_SERVICE_UNAVAILABLE);
		}
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
