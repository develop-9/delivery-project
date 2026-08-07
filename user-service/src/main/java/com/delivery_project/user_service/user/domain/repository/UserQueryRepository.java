package com.delivery_project.user_service.user.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.delivery_project.user_service.user.domain.entity.Role;
import com.delivery_project.user_service.user.domain.entity.User;

/**
 * 사용자 쿼리 측 포트. UserQueryService가 쓴다.
 *
 * 여기 있는 조회는 상태를 바꾸기 위한 것이 아니라 화면에 보여주기 위한 것이다.
 * 상태 변경/불변식 검사를 위한 조회는 UserCommandRepository에 있다.
 */
public interface UserQueryRepository {

	/** 승인 대기자 전체 조회 (문서 3번). */
	Page<User> findAllPending(Pageable pageable);

	/** 특정 허브의 승인 대기자 조회 (문서 3번). */
	Page<User> findPendingByHub(UUID hubId, Pageable pageable);

	Optional<User> findById(UUID id);

	/** Internal API 배치 조회용. 존재하지 않는 id는 결과에서 제외된다. */
	List<User> findAllByIds(Collection<UUID> ids);

	/**
	 * 허브당 담당자는 1명이라는 팀 결정 기준(문서 3번, Internal API)이지만 DB 제약으로 강제되진
	 * 않으므로, 여러 건이 매칭될 가능성을 배제하지 않고 List로 받는다. createdAt 오름차순으로
	 * 정렬돼 반환된다.
	 */
	List<User> findByHubIdAndRole(UUID hubId, Role role);

	/** 사용자 목록 동적 조합 검색 (approvalStatus/role/hubId/companyId, 문서 3번). */
	Page<User> search(UserSearchCondition condition, Pageable pageable);
}
