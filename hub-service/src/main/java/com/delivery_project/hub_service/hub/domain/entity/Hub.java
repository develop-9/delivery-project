package com.delivery_project.hub_service.hub.domain.entity;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.domain.Persistable;

import com.delivery_project.hub_service.global.common.BaseDeletableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 허브. {@code MAIN} 의 {@code parentHubId} 는 자기 자신이다 (D1).
 *
 * <p>그래서 식별자를 DB 가 아니라 애플리케이션이 발급한다. {@code @GeneratedValue} 를 쓰면
 * INSERT 후에야 id 를 알 수 있어 자기참조 값을 같은 INSERT 문에 담을 수 없고,
 * INSERT → UPDATE 2단계가 되어 {@code ck_hub_type_parent} CHECK 제약을 걸 수 없다.
 *
 * <p>할당 식별자를 쓰면 {@code save()} 가 새 엔티티도 merge 로 처리해 불필요한 SELECT 가 붙으므로
 * {@link Persistable} 을 구현해 신규 여부를 직접 알려준다.
 */
@Entity
@Table(name = "p_hubs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at IS NULL")
public class Hub extends BaseDeletableEntity implements Persistable<UUID> {

	@Id
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	@Column(name = "address", nullable = false, length = 255)
	private String address;

	@Column(name = "latitude", precision = 10, scale = 7)
	private BigDecimal latitude;

	@Column(name = "longitude", precision = 10, scale = 7)
	private BigDecimal longitude;

	@Enumerated(EnumType.STRING)
	@Column(name = "hub_type", nullable = false, length = 20)
	private HubType hubType;

	@Column(name = "parent_hub_id")
	private UUID parentHubId;

	@Transient
	private boolean isNew = true;

	private Hub(String name, String address, BigDecimal latitude, BigDecimal longitude, HubType hubType) {
		this.id = UUID.randomUUID();
		this.name = name;
		this.address = address;
		this.latitude = latitude;
		this.longitude = longitude;
		this.hubType = hubType;
	}

	/** 중앙 허브. {@code parentHubId} 를 영속화 전에 자기 자신으로 채운다. */
	public static Hub createMain(String name, String address, BigDecimal latitude, BigDecimal longitude) {
		Hub hub = new Hub(name, address, latitude, longitude, HubType.MAIN);
		hub.parentHubId = hub.id;
		return hub;
	}

	/** 일반 허브. {@code parentHubId} 는 {@code MAIN} 허브를 가리켜야 하며 검증은 Service 가 한다. */
	public static Hub createSub(String name, String address, BigDecimal latitude, BigDecimal longitude,
			UUID parentHubId) {
		Hub hub = new Hub(name, address, latitude, longitude, HubType.SUB);
		hub.parentHubId = parentHubId;
		return hub;
	}

	@Override
	public boolean isNew() {
		return isNew;
	}

	@PostPersist
	@PostLoad
	private void markNotNew() {
		this.isNew = false;
	}
}
