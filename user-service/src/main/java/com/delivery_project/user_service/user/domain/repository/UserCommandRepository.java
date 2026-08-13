package com.delivery_project.user_service.user.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.delivery_project.user_service.user.domain.entity.User;

/**
 * 사용자 커맨드 측 포트. AuthCommandService/UserCommandService가 쓴다.
 *
 * 여기 있는 조회는 화면에 보여주기 위한 것이 아니라 상태를 바꾸기 위해 애그리거트를 불러오거나
 * 불변식을 검사하는 용도다. 목록·페이지 조회는 UserQueryRepository에 있다.
 */
public interface UserCommandRepository {

	User save(User user);

	Optional<User> findById(UUID id);

	Optional<User> findByUsername(String username);

	boolean existsByUsername(String username);

	boolean existsBySlackId(String slackId);

	/** 삭제 시 마지막 MASTER를 지우는 걸 막기 위한 활성 MASTER 수 확인용. */
	long countActiveMasters();

	/**
	 * countActiveMasters()와 달리 활성 MASTER 행에 비관적 락(SELECT ... FOR UPDATE)을 건다.
	 * "마지막 MASTER인지 확인 → 정지/삭제 실행" 사이에 동시에 다른 MASTER를 정지/삭제하는
	 * 요청이 끼어들어 활성 MASTER가 0명이 되는 걸 막기 위해, 이 확인과 실제 상태 변경이
	 * 같은 트랜잭션 안에서 원자적으로 이뤄져야 하는 곳(suspend, commitDelete)에서만 쓴다.
	 */
	long countActiveMastersForUpdate();
}
