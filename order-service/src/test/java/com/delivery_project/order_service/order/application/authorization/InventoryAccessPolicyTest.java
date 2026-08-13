package com.delivery_project.order_service.order.application.authorization;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.global.security.JwtPrincipal;
import com.delivery_project.order_service.global.security.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 재고 변경 권한.
 *
 * <p>등록·입고·실사 보정·삭제는 <b>실물 수량</b>을 바꾼다. 인증만 통과하면 누구나 수량을 덮어쓸 수
 * 있었던 상태를 막는다.
 */
class InventoryAccessPolicyTest {

	private final InventoryAccessPolicy policy = new InventoryAccessPolicy();

	private JwtPrincipal principal(Role role) {
		return new JwtPrincipal(UUID.randomUUID(), role);
	}

	@ParameterizedTest
	@EnumSource(value = Role.class, names = {"MASTER", "HUB_MANAGER"})
	@DisplayName("재고 담당 역할은 수량을 바꿀 수 있다")
	void stockManagersCanWrite(Role role) {
		assertThatCode(() -> policy.validateWritable(principal(role)))
				.doesNotThrowAnyException();
	}

	@ParameterizedTest
	@EnumSource(value = Role.class, names = {"COMPANY_MANAGER", "DELIVERY_MANAGER"})
	@DisplayName("업체·배송 담당자는 재고를 바꿀 수 없다")
	void othersCannotWrite(Role role) {
		// 유효한 토큰만 있으면 아무 재고나 덮어쓸 수 있던 구멍을 막는다
		assertThatThrownBy(() -> policy.validateWritable(principal(role)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.AUTH_FORBIDDEN);
	}

	@Test
	@DisplayName("역할을 알 수 없으면 막는다")
	void unknownRoleCannotWrite() {
		// 토큰에 모르는 role 이 실려 파싱에서 null 이 된 경우.
		// 판별 못 하는 것을 통과시키면 검사 자체가 무의미해진다
		assertThatThrownBy(() -> policy.validateWritable(new JwtPrincipal(UUID.randomUUID(), null)))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("인증 주체가 없으면 통과시킨다 — 인증을 끈 로컬 실행용")
	void noPrincipalPasses() {
		assertThatCode(() -> policy.validateWritable(null)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("감사 주체는 인증 주체가 없으면 시스템 ID 로 대체한다")
	void auditorFallsBackToSystemId() {
		UUID systemId = UUID.randomUUID();
		UUID callerId = UUID.randomUUID();

		assertThatCode(() -> {
			assert policy.callerOrSystem(null, systemId).equals(systemId);
			assert policy.callerOrSystem(new JwtPrincipal(callerId, Role.MASTER), systemId).equals(callerId);
		}).doesNotThrowAnyException();
	}
}
