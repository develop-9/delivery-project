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
public interface UserRepository {

	User save(User user);

	Optional<User> findById(UUID id);

	Optional<User> findByUsername(String username);

	boolean existsByUsername(String username);

	boolean existsBySlackId(String slackId);
}
