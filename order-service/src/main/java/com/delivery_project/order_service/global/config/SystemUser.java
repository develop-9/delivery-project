package com.delivery_project.order_service.global.config;

import java.util.UUID;

/**
 * 인증 주체가 없을 때 사용하는 시스템 사용자.
 *
 * <p>TODO JWT 파싱 필터가 들어오면 인증 없는 요청은 401 로 막히므로 이 상수는 제거한다.
 * 그전까지는 감사 컬럼(NOT NULL)과 주문 요청자를 채우기 위해 사용한다.
 */
public final class SystemUser {

	public static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

	private SystemUser() {
	}

	/** 인증 주체가 없으면 시스템 사용자로 대체한다 */
	public static UUID orSystem(UUID callerId) {
		return callerId != null ? callerId : ID;
	}
}
