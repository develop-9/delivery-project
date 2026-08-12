package com.delivery_project.order_service.order.application.authorization;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.global.security.JwtPrincipal;
import com.delivery_project.order_service.global.security.Role;
import com.delivery_project.order_service.order.domain.entity.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 주문 접근 권한 규칙.
 *
 * <p>인증만 있고 인가가 없으면 로그인한 아무나 남의 주문을 조회·수정·취소할 수 있다.
 * 여기서 <b>읽기는 404, 쓰기는 403</b> 이라는 응답 차이도 함께 고정한다.
 */
class OrderAccessPolicyTest {

	private final OrderAccessPolicy policy = new OrderAccessPolicy();

	private final UUID ownerId = UUID.randomUUID();
	private final UUID strangerId = UUID.randomUUID();

	private Order order() {
		return Order.builder()
				.supplierCompanyId(UUID.randomUUID())
				.receiverCompanyId(UUID.randomUUID())
				.receiverUserId(ownerId)
				.requestDetails("오전 중 배송 부탁드립니다")
				.build();
	}

	private JwtPrincipal principal(UUID userId, Role role) {
		return new JwtPrincipal(userId, role);
	}

	@Test
	@DisplayName("수령인 본인은 자기 주문을 볼 수 있다")
	void ownerCanRead() {
		assertThatCode(() -> policy.validateReadable(order(), principal(ownerId, Role.COMPANY_MANAGER)))
				.doesNotThrowAnyException();
	}

	@Test
	@DisplayName("남의 주문 조회는 404 로 막는다 — 존재 자체를 숨긴다")
	void strangerCannotRead() {
		// 403 을 주면 상태코드 차이만으로 "그 ID 의 주문이 있다"를 알아낼 수 있다
		assertThatThrownBy(() -> policy.validateReadable(order(), principal(strangerId, Role.COMPANY_MANAGER)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.ORDER_NOT_FOUND);
	}

	@Test
	@DisplayName("남의 주문 변경은 403 으로 막는다")
	void strangerCannotWrite() {
		// 이미 주문의 존재를 아는 요청이다. 404 를 주면 "사라졌다"로 오해해 다시 만들려 든다
		assertThatThrownBy(() -> policy.validateWritable(order(), principal(strangerId, Role.COMPANY_MANAGER)))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.AUTH_FORBIDDEN);
	}

	@Test
	@DisplayName("MASTER 는 남의 주문도 보고 바꿀 수 있다")
	void masterHasFullAccess() {
		JwtPrincipal master = principal(strangerId, Role.MASTER);

		assertThatCode(() -> policy.validateReadable(order(), master)).doesNotThrowAnyException();
		assertThatCode(() -> policy.validateWritable(order(), master)).doesNotThrowAnyException();
	}

	@Test
	@DisplayName("HUB_MANAGER 라도 남의 주문은 아직 못 본다")
	void hubManagerIsNotPrivilegedYet() {
		// 토큰에 담당 허브가 없어 "내 허브 주문인지" 를 판별할 수 없다.
		// 판별 못 하는 것을 통과시키면 사실상 전체 열람이 된다
		assertThatThrownBy(() -> policy.validateReadable(order(), principal(strangerId, Role.HUB_MANAGER)))
				.isInstanceOf(BusinessException.class);
	}

	@Test
	@DisplayName("MASTER 만 전체 주문을 검색할 수 있다")
	void onlyMasterSearchesAllOrders() {
		assertThat(policy.canSeeAllOrders(principal(strangerId, Role.MASTER))).isTrue();
		assertThat(policy.canSeeAllOrders(principal(strangerId, Role.COMPANY_MANAGER))).isFalse();
		assertThat(policy.canSeeAllOrders(principal(strangerId, Role.HUB_MANAGER))).isFalse();
	}

	@Test
	@DisplayName("인증 주체가 없으면 통과시킨다 — 인증을 끈 로컬 실행용")
	void noPrincipalPasses() {
		assertThatCode(() -> policy.validateReadable(order(), null)).doesNotThrowAnyException();
		assertThat(policy.canSeeAllOrders(null)).isTrue();
	}
}
