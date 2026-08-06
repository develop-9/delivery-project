package com.delivery_project.hub_service.hub.domain.entity;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.SQLRestriction;

import com.delivery_project.hub_service.global.common.BaseDeletableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 허브 간 이동정보. 한 행이 구간 하나(출발 → 도착)를 나타낸다.
 *
 * <p>경로는 <b>유향</b>이다 (D3). {@code A→B} 와 {@code B→A} 는 별개 행이며,
 * 방향에 따라 소요 시간이 다를 수 있어 한 행으로 합치지 않는다.
 *
 * <p>출발·도착 허브는 구간의 정체성이라 수정할 수 없다. 구간을 바꾸려면 삭제 후 재등록한다.
 * 그래서 두 컬럼은 {@code updatable = false} 이고, 수정 가능한 것은 거리·시간뿐이다.
 *
 * <p>허브를 {@code @ManyToOne} 이 아니라 {@code UUID} 로 들고 있다. 경로 산출은 그래프 탐색이 아니라
 * {@code parentHubId} 계층 규칙으로 하고(D2) 이 테이블은 거리·시간 조회용이라 조인이 필요 없다.
 *
 * <p>{@link Hub} 와 달리 <b>식별자를 {@code @GeneratedValue} 로 발급한다.</b>
 * {@code Hub} 가 할당 식별자를 쓰는 건 {@code MAIN} 의 자기참조(D1) 때문에 INSERT 전에 자기 ID 를
 * 알아야 해서지, 이 테이블에는 그런 제약이 없다. 그래서 {@code Persistable} 구현도 필요 없다 —
 * {@code save()} 가 {@code id == null} 로 신규를 판정해 {@code persist()} 로 간다.
 *
 * <p><b>{@code (departure, arrival)} 유니크 제약을 여기에 선언하지 않는다.</b>
 * 논리 삭제를 쓰므로 제약이 {@code WHERE deleted_at IS NULL} 부분 인덱스여야 하는데
 * JPA 로는 표현할 수 없다. 여기서 {@code uniqueConstraints} 로 선언하면 부분 조건이 빠진 전체 유니크가 되어
 * <b>삭제한 구간을 다시 등록할 수 없게 된다.</b> 제약은 {@code docs/api/00_common.md} 의 DDL 로 건다.
 *
 * <p>{@code departure != arrival} 검증도 Service 가 한다 ({@code 400 SAME_HUB_NOT_ALLOWED}).
 * DB 에는 {@code ck_hub_route_not_self} CHECK 가 최후 방어선으로 걸린다.
 */
@Entity
@Table(name = "p_hub_routes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class HubRoute extends BaseDeletableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	@Column(name = "departure_hub_id", nullable = false, updatable = false)
	private UUID departureHubId;

	@Column(name = "arrival_hub_id", nullable = false, updatable = false)
	private UUID arrivalHubId;

	@Column(name = "distance_km", nullable = false, precision = 10, scale = 2)
	private BigDecimal distanceKm;

	@Column(name = "duration_min", nullable = false)
	private int durationMin;

	private HubRoute(UUID departureHubId, UUID arrivalHubId, BigDecimal distanceKm, int durationMin) {
		this.departureHubId = departureHubId;
		this.arrivalHubId = arrivalHubId;
		this.distanceKm = distanceKm;
		this.durationMin = durationMin;
	}

	/**
	 * 구간 생성. 거리·시간은 지도 API 산출값이든 요청 지정값이든 Service 가 확정해서 넘긴다 (D8).
	 *
	 * <p>Hub-and-Spoke 구간 유효성과 출발·도착 동일 여부는 Service 가 검증한다.
	 */
	public static HubRoute create(UUID departureHubId, UUID arrivalHubId, BigDecimal distanceKm,
			int durationMin) {
		return new HubRoute(departureHubId, arrivalHubId, distanceKm, durationMin);
	}

	/**
	 * 거리·시간 갱신. 부분 수정처럼 보이지만 Service 가 최종값 두 개를 모두 확정한 뒤 호출한다.
	 *
	 * <p>PATCH 는 빈 바디면 지도 API 재계산, 일부만 오면 지정 필드 우선이라 (문서 9번)
	 * "무엇으로 덮을지" 판단은 전부 Service 몫이고 엔티티는 결과만 받는다.
	 */
	public void updateMetrics(BigDecimal distanceKm, int durationMin) {
		this.distanceKm = distanceKm;
		this.durationMin = durationMin;
	}
}
