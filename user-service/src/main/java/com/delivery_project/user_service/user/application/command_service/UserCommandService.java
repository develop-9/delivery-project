package com.delivery_project.user_service.user.application.command_service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.user.application.command.UserApproveCommand;
import com.delivery_project.user_service.user.application.command.UserDeleteCommand;
import com.delivery_project.user_service.user.application.command.UserReinstateCommand;
import com.delivery_project.user_service.user.application.command.UserRejectCommand;
import com.delivery_project.user_service.user.application.command.UserSuspendCommand;
import com.delivery_project.user_service.user.application.command.UserUpdateMeCommand;
import com.delivery_project.user_service.user.application.persistence_service.UserPersistenceService;
import com.delivery_project.user_service.user.application.port.DeliveryManagerPort;
import com.delivery_project.user_service.user.application.support.CallerResolver;
import com.delivery_project.user_service.user.application.result.UserApproveResult;
import com.delivery_project.user_service.user.application.result.UserDeleteResult;
import com.delivery_project.user_service.user.application.result.UserReinstateResult;
import com.delivery_project.user_service.user.application.result.UserRejectResult;
import com.delivery_project.user_service.user.application.result.UserSuspendResult;
import com.delivery_project.user_service.user.application.result.UserUpdateMeResult;
import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.entity.User;
import com.delivery_project.user_service.user.domain.repository.RefreshTokenRepository;
import com.delivery_project.user_service.user.domain.repository.UserCommandRepository;
import com.delivery_project.user_service.user.domain.repository.UserInvalidationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserCommandService {

	private final UserCommandRepository userCommandRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final UserInvalidationRepository userInvalidationRepository;
	private final UserPersistenceService userPersistenceService;
	private final DeliveryManagerPort deliveryManagerPort;
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

		target.approve(caller.getId());
		log.info("[User] 회원가입 승인 완료 targetUserId={} approvedBy={}", command.targetUserId(), caller.getId());

		return UserApproveResult.from(target);
	}

	public UserRejectResult reject(UUID callerId, UserRejectCommand command) {
		User caller = callerResolver.resolve(callerId);
		User target = userCommandRepository.findById(command.targetUserId())
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		validatePermission(caller, target, ErrorCode.REJECT_USER_FORBIDDEN);

		target.reject();
		log.info("[User] 회원가입 거절 완료 targetUserId={} rejectedBy={}", command.targetUserId(), caller.getId());

		return UserRejectResult.from(target);
	}

	/**
	 * 정지는 delete()와 달리 다른 서비스로의 Feign 호출이 없어 트랜잭션 경계를 따로 분리할
	 * 필요가 없다. Access Token 무효화는 Redis에 직접 쓰지 않고 같은 트랜잭션 안에서 아웃박스에
	 * 기록되므로(UserInvalidationRepositoryImpl 참고), 커밋 순서를 따로 신경 쓸 필요가 없다.
	 */
	public UserSuspendResult suspend(UUID callerId, UserSuspendCommand command) {
		User caller = callerResolver.resolve(callerId);
		if (!caller.isMaster()) {
			throw new BusinessException(ErrorCode.SUSPEND_USER_FORBIDDEN);
		}

		User target = userCommandRepository.findById(command.targetUserId())
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		if (target.getApprovalStatus() != ApprovalStatus.APPROVED) {
			throw new BusinessException(ErrorCode.USER_NOT_SUSPENDABLE);
		}

		// countActiveMasters() 대신 락을 거는 버전을 쓴다 — "마지막 MASTER인지 확인"과 "정지 실행"이
		// 이 메서드 전체를 감싼 같은 트랜잭션 안에서 원자적으로 이뤄져야, 동시에 다른 MASTER를
		// 정지/삭제하는 요청이 끼어들어 활성 MASTER가 0명이 되는 걸 막을 수 있다.
		if (target.getRole() == Role.MASTER && userCommandRepository.countActiveMastersForUpdate() <= 1) {
			throw new BusinessException(ErrorCode.LAST_MASTER_SUSPEND_FORBIDDEN);
		}

		target.suspend();
		refreshTokenRepository.deleteByUserId(target.getId());
		userInvalidationRepository.invalidate(target.getId(), Instant.now());
		log.info("[User] 계정 정지 완료 targetUserId={} suspendedBy={}", command.targetUserId(), caller.getId());

		return UserSuspendResult.from(target);
	}

	/**
	 * 정지 해제는 CallerResolver의 승인 상태 검증(APPROVED만 통과)이 다시 걸리게 하는 것뿐이라,
	 * 삭제/정지와 달리 Redis에 남길 것이 없다 — 무효화 기록을 지우는 게 아니라 그냥 만료를
	 * 기다리면 되고, 재승인 이후엔 어차피 새로 로그인해서 새 토큰을 받아야 한다.
	 */
	public UserReinstateResult reinstate(UUID callerId, UserReinstateCommand command) {
		User caller = callerResolver.resolve(callerId);
		if (!caller.isMaster()) {
			throw new BusinessException(ErrorCode.REINSTATE_USER_FORBIDDEN);
		}

		User target = userCommandRepository.findById(command.targetUserId())
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		if (target.getApprovalStatus() != ApprovalStatus.SUSPENDED) {
			throw new BusinessException(ErrorCode.USER_NOT_SUSPENDED);
		}

		target.reinstate();
		log.info("[User] 계정 정지 해제 완료 targetUserId={} reinstatedBy={}", command.targetUserId(), caller.getId());

		return UserReinstateResult.from(target);
	}

	/**
	 * 클래스 레벨 @Transactional을 이 메서드에서만 명시적으로 비활성화한다(NOT_SUPPORTED).
	 * syncDeleteDeliveryManager()의 동기 Feign 호출이 DB 트랜잭션 밖에서 실행되도록 하기 위함 —
	 * delivery-service가 느리거나 다운되면 DB 커넥션을 붙잡은 채로 대기하게 되어 커넥션 풀
	 * 고갈로 이어질 수 있고, Feign은 성공했는데 이후 DB 커밋이 실패하면 두 서비스 데이터가
	 * 어긋난다. 실제 DB 쓰기는 userPersistenceService.commitDelete()의 별도 트랜잭션에 담는다.
	 *
	 * TODO: Feign 성공 후 DB 커밋 실패 시의 정합성 위반 가능성은 지금 감수 중인 트레이드오프다
	 * (커넥션 풀 고갈이라는 더 나쁜 대안 대신 택함). 발생 확률은 낮지만 없앨 수는 있다 — Access
	 * Token 무효화에 쓴 것과 같은 아웃박스 패턴을 이 Feign 호출에도 적용해서, DB 커밋을 먼저
	 * 확정시키고 Delivery Service 삭제 요청은 커밋 후 비동기로 안정적으로 보내는 방향을 검토.
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public UserDeleteResult delete(UUID callerId, UserDeleteCommand command) {
		User caller = callerResolver.resolve(callerId);
		if (!caller.isMaster()) {
			throw new BusinessException(ErrorCode.DELETE_USER_FORBIDDEN);
		}

		User target = userCommandRepository.findById(command.targetUserId())
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		// 마지막 MASTER 확인은 여기서 하지 않고 commitDelete()의 트랜잭션 안에서 락을 걸고
		// 한다(아래 UserPersistenceService.commitDelete() 참고) — MASTER는 DELIVERY_MANAGER
		// 동기화 대상이 아니라 이 순서 변경이 Feign 호출 여부에 영향을 주지 않는다.

		// COMPANY_MANAGER는 Company Service에 연동 대상 레코드가 없어 확인 결과 연동 불필요로 확정됨.
		if (target.getRole() == Role.DELIVERY_MANAGER) {
			syncDeleteDeliveryManager(target.getId());
		}

		UserDeleteResult result = userPersistenceService.commitDelete(command.targetUserId(), caller.getId());

		log.info("[User] 사용자 삭제 완료 targetUserId={} deletedBy={}", command.targetUserId(), caller.getId());

		return result;
	}

	/**
	 * Delivery Service 연동이 (레코드가 원래 없는 경우 제외하고) 실패하면 사용자 삭제 자체를 막는다.
	 * 정리가 안 된 배송담당자 레코드를 Delivery Service 쪽에 활성 상태로 남겨두면, User Service에서는
	 * 이미 삭제되어 로그인도 안 되는 사람에게 라운드로빈으로 새 배송이 배정될 수 있어 "최선 노력"보다
	 * 안전한 실패(차단) 쪽을 택함. 재시도 장치가 없어 한 번 어긋나면 계속 그 상태로 남는 것도 이유.
	 * 진행 중인 배송에 배정된 경우(DELIVERY_MANAGER_HAS_ACTIVE_DELIVERY)와 Delivery Service 장애
	 * (DELIVERY_SERVICE_UNAVAILABLE)는 DeliveryManagerPort가 이미 구분해서 던지므로, 여기서는
	 * BusinessException이 올라오면 그대로 전파하기만 하면 된다.
	 */
	private void syncDeleteDeliveryManager(UUID userId) {
		deliveryManagerPort.deleteByUserId(userId);
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
