package com.delivery_project.hub_service.hub.domain.repository;

import java.util.Optional;
import java.util.UUID;

import com.delivery_project.hub_service.hub.domain.entity.Hub;

/**
 * 허브 <b>커맨드</b> 측 포트. {@code HubCommandService} 가 쓴다.
 *
 * <p>여기 있는 조회는 화면에 보여주기 위한 것이 아니라 <b>상태를 바꾸기 위해 애그리거트를 불러오거나
 * 불변식을 검사하는</b> 용도다. 그래서 커맨드 측에 둔다. 목록·검색은 {@link HubQueryRepository} 에 있다.
 *
 * <p>모든 메서드는 {@code deleted_at IS NULL} 인 행만 본다. 엔티티의
 * {@code @SQLRestriction} 이 붙여주므로 구현체가 조건을 따로 쓰지 않는다.
 */
public interface HubCommandRepository {

	Hub save(Hub hub);

	/** 수정·삭제 대상 애그리거트 로딩. */
	Optional<Hub> findById(UUID hubId);

	/** 허브명 중복 검사 ({@code 409 DUPLICATE_HUB_NAME}). */
	boolean existsByName(String name);

	/**
	 * 하위 허브 수 ({@code 409 HUB_HAS_CHILDREN}).
	 *
	 * <p><b>자기 자신은 세지 않는다.</b> D1 로 {@code MAIN} 은 {@code parentHubId} 가 자기 자신이라
	 * 빼지 않으면 중앙 허브는 하위가 없어도 항상 1 이 나와 영원히 삭제되지 않는다.
	 */
	long countChildren(UUID hubId);

	/**
	 * 살아 있는 허브 수.
	 *
	 * <p>이동정보 삭제 응답의 {@code affectedHubPairCount} 산출에 쓴다 —
	 * SUB 가 걸린 구간이 사라지면 그 허브를 한쪽 끝으로 하는 모든 쌍의 경로가 끊긴다.
	 */
	long countAll();
}
