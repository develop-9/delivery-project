package com.delivery_project.hub_service.hub.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.delivery_project.hub_service.hub.domain.entity.Hub;

/**
 * 허브 <b>쿼리</b> 측 포트. {@code HubQueryService} 가 쓴다.
 *
 * <p>CQRS 를 코드 단위로만 나눈다 — 읽기 전용 DB 나 별도 읽기 모델을 두지 않고, 같은 테이블을
 * 같은 트랜잭션 경계 안에서 읽는다. 나누는 이유는 저장소가 아니라 <b>변경 경로와 조회 경로가
 * 서로의 사정에 끌려다니지 않게</b> 하기 위해서다. 조회 성능을 위해 인덱스나 쿼리를 바꿔도
 * 커맨드 측 불변식 검사는 그대로 남는다.
 *
 * <p>{@link #findById} 가 {@link HubRepository#findById} 와 겹쳐 보이지만 목적이 다르다.
 * 이쪽은 응답으로 내보낼 데이터를 읽고, 저쪽은 상태를 바꿀 애그리거트를 불러온다.
 * 나중에 이쪽만 엔티티 대신 프로젝션을 반환하도록 바꿀 수 있다.
 */
public interface HubQueryRepository {

	/** 단건 조회 (문서 2번 · 내부 12번). */
	Optional<Hub> findById(UUID hubId);

	/** 목록·검색 (문서 3번). */
	Page<Hub> search(HubSearchCondition condition, Pageable pageable);

	/**
	 * 다건 조회 (내부 13번).
	 *
	 * <p>일부만 존재해도 에러가 아니다. 찾은 것만 담아 돌려주고 없는 ID 판정은 Service 가 한다.
	 */
	List<Hub> findAllByIds(Collection<UUID> hubIds);
}
