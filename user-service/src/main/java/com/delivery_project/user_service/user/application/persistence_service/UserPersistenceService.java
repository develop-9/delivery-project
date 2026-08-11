package com.delivery_project.user_service.user.application.persistence_service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.user.application.port.MasterBootstrapLockPort;
import com.delivery_project.user_service.user.application.result.UserDeleteResult;
import com.delivery_project.user_service.user.domain.entity.ApprovalStatus;
import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.entity.User;
import com.delivery_project.user_service.user.domain.repository.RefreshTokenRepository;
import com.delivery_project.user_service.user.domain.repository.UserCommandRepository;
import com.delivery_project.user_service.user.domain.repository.UserInvalidationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AuthCommandService/UserCommandService가 Hub/Company/Delivery Service로 Feign 호출을
 * 하는 동안에는 이 클래스의 메서드를 부르지 않는다 — 그래서 여기 메서드들은 전부 자기 자신만으로
 * 완결된 짧은 트랜잭션이고, 외부 서비스 포트를 전혀 모른다.
 *
 * 호출부(signup()/delete())가 Feign 호출을 트랜잭션 밖에서 하도록 NOT_SUPPORTED로 바꾸면서,
 * 이 클래스의 메서드는 항상 새 트랜잭션에서 시작한다(호출 시점에 이어받을 트랜잭션이 없으므로).
 * 그래서 Feign 호출 이전에 조회한 엔티티를 그대로 넘겨받지 않고, 필요한 값은 ID로 받아 이
 * 트랜잭션 안에서 다시 조회한다 — User Service는 이미 open-in-view: false라 다른 트랜잭션에서
 * 조회한 엔티티를 준영속 상태로 넘겨받으면 save() 없이는 반영되지 않는다. (신규 생성은 예외다 —
 * commitSignup()이 받는 User는 아직 한 번도 저장된 적 없는 트랜지언트 엔티티라 준영속 상태
 * 자체가 없다.)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserPersistenceService {

	private final UserCommandRepository userCommandRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final UserInvalidationRepository userInvalidationRepository;
	private final MasterBootstrapLockPort masterBootstrapLockPort;

	/**
	 * 활성 MASTER가 한 명도 없으면 아무도 승인해줄 수 없어 첫 MASTER 가입자가 영구히 PENDING에
	 * 머무르는 부트스트랩 데드락이 생긴다. 이 경우에 한해 가입과 동시에 자동 승인한다.
	 *
	 * 이미 있는 MASTER 행에 거는 비관적 락(countActiveMastersForUpdate)은 "아직 MASTER가
	 * 한 명도 없는" 이 상황엔 못 쓴다 — 잠글 행 자체가 없어서, 두 명이 동시에 최초 MASTER로
	 * 가입하면 둘 다 count()==0을 보고 둘 다 자동 승인될 수 있다. advisory lock으로 이
	 * 확인~승인 구간 자체를 직렬화해서 막는다(트랜잭션이 끝나면 자동 해제).
	 */
	public User commitSignup(User user) {
		if (user.getRole() == Role.MASTER) {
			masterBootstrapLockPort.lock();
			if (userCommandRepository.countActiveMasters() == 0) {
				user.approveAsInitialMaster();
			}
		}

		return saveUser(user);
	}

	/**
	 * existsByUsername/existsBySlackId 사전 체크와 저장 사이에 동시에 같은 값으로 가입 요청이
	 * 들어오면, 사전 체크를 통과하고도 DB의 부분 유니크 인덱스(삭제되지 않은 행에만 적용 —
	 * User.java, UserTableSchemaInitializer 참고)에서 걸릴 수 있다. 이 경우를 여기서 구체적인
	 * ErrorCode로 변환한다(그 외 제약 위반은 GlobalExceptionHandler의 일반 처리로 위임).
	 */
	private User saveUser(User user) {
		try {
			return userCommandRepository.save(user);
		} catch (DataIntegrityViolationException e) {
			String message = e.getMessage();
			if (message != null && message.contains("(username)")) {
				throw new BusinessException(ErrorCode.USER_DUPLICATE_USERNAME);
			}
			if (message != null && message.contains("(slack_id)")) {
				throw new BusinessException(ErrorCode.USER_DUPLICATE_SLACK_ID);
			}
			throw e;
		}
	}

	/**
	 * delete()가 Feign 호출을 끝낸 뒤에만 부르는 실제 DB 쓰기. ID로 다시 조회하는 이유는 클래스
	 * 상단 설명 참고.
	 *
	 * 마지막 MASTER 확인도 일부러 여기서 한다 — countActiveMastersForUpdate()가 활성 MASTER
	 * 행에 거는 락이 이 트랜잭션이 커밋될 때까지 유지돼야, 동시에 다른 MASTER를 정지/삭제하는
	 * 요청이 끼어들어 활성 MASTER가 0명이 되는 걸 막을 수 있다(suspend()와 동일한 이유).
	 */
	public UserDeleteResult commitDelete(UUID targetUserId, UUID callerId) {
		User target = userCommandRepository.findById(targetUserId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		if (target.getRole() == Role.MASTER
				&& target.getApprovalStatus() == ApprovalStatus.APPROVED
				&& userCommandRepository.countActiveMastersForUpdate() <= 1) {
			throw new BusinessException(ErrorCode.LAST_MASTER_DELETE_FORBIDDEN);
		}

		target.delete(callerId);
		refreshTokenRepository.deleteByUserId(target.getId());
		// Refresh Token 삭제만으로는 이미 발급된 Access Token까지 막지 못하므로, 무효화 시각을
		// 별도로 기록해서 Gateway가 만료 전 토큰도 차단할 수 있게 한다(Gateway JWT 인증 필터 참고).
		// Redis에 직접 쓰지 않고 같은 트랜잭션 안에서 아웃박스에 기록되므로(UserInvalidationRepositoryImpl
		// 참고), 이 트랜잭션이 롤백되면 무효화 기록도 함께 롤백된다.
		userInvalidationRepository.invalidate(target.getId(), Instant.now());

		return UserDeleteResult.from(target);
	}
}
