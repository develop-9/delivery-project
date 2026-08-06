package com.delivery_project.user_service.user.domain.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.delivery_project.user_service.user.domain.entity.User;

/**
 * 사용자 쿼리 측 포트. UserQueryService가 쓴다.
 *
 * 여기 있는 조회는 상태를 바꾸기 위한 것이 아니라 화면에 보여주기 위한 것이다.
 * 상태 변경/불변식 검사를 위한 조회는 UserRepository에 있다.
 */
public interface UserQueryRepository {

	/** 승인 대기자 전체 조회 (문서 3번). */
	Page<User> findAllPending(Pageable pageable);

	/** 특정 허브의 승인 대기자 조회 (문서 3번). */
	Page<User> findPendingByHub(UUID hubId, Pageable pageable);
}
