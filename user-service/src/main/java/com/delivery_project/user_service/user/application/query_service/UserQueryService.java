package com.delivery_project.user_service.user.application.query_service;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.user.application.support.CallerResolver;
import com.delivery_project.user_service.user.application.result.InternalUserResult;
import com.delivery_project.user_service.user.application.result.InternalUserSlackResult;
import com.delivery_project.user_service.user.application.result.UserDetailResult;
import com.delivery_project.user_service.user.application.result.UserListResult;
import com.delivery_project.user_service.user.application.result.UserPendingResult;
import com.delivery_project.user_service.user.domain.entity.Role;
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

	/**
	 * 권한 체크를 조회보다 먼저 한다(delete()와 동일 순서). targetUserId만으로 본인 여부를
	 * 판단할 수 있어 대상을 먼저 불러올 필요가 없고, 순서를 바꾸면 권한 없는 호출자가 404/403
	 * 상태코드 차이로 대상 존재 여부를 알아낼 수 있는 정보 노출도 같이 막힌다.
	 */
	public UserDetailResult getById(UUID callerId, UUID targetUserId) {
		User caller = callerResolver.resolve(callerId);
		if (!caller.isMaster() && !caller.isSelf(targetUserId)) {
			throw new BusinessException(ErrorCode.READ_USER_FORBIDDEN, "사용자 조회 권한이 없습니다.");
		}

		User target = userQueryRepository.findById(targetUserId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		return UserDetailResult.from(target);
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

	public InternalUserResult getInternalUser(UUID userId) {
		User user = userQueryRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		return InternalUserResult.from(user);
	}

	/** 존재하지 않는 id는 결과에서 조용히 제외한다(문서 3번, N+1 방지용 배치 조회). */
	public List<InternalUserResult> getInternalUsersBatch(Collection<UUID> ids) {
		return userQueryRepository.findAllByIds(ids).stream()
				.map(InternalUserResult::from)
				.toList();
	}

	/** 허브 관리자는 허브당 1명이라는 팀 결정 기준(문서 3번, Internal API). */
	/**
	 * 허브당 담당자는 1명이라는 가정이 DB로 강제되진 않으므로, 여러 건이 매칭되면 첫 번째를 반환한다
	 * (예외 대신 결정적인 값을 돌려주는 쪽을 택함 — 이 가정이 깨진 데이터가 있어도 API가 죽지 않게).
	 */
	public InternalUserResult getInternalUserByHubAndRole(UUID hubId, Role role) {
		List<User> users = userQueryRepository.findByHubIdAndRole(hubId, role);
		if (users.isEmpty()) {
			throw new BusinessException(ErrorCode.HUB_MANAGER_NOT_FOUND);
		}
		return InternalUserResult.from(users.get(0));
	}

	public InternalUserSlackResult getInternalUserSlack(UUID userId) {
		User user = userQueryRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		return InternalUserSlackResult.from(user);
	}
}
