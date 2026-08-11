package com.delivery_project.order_service.order.application.authorization;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.global.security.JwtPrincipal;
import com.delivery_project.order_service.global.security.Role;
import com.delivery_project.order_service.order.domain.entity.Order;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 주문에 누가 접근할 수 있는지 한 곳에서 정한다.
 *
 * <p>규칙을 서비스마다 흩어 놓으면 조회는 막았는데 수정은 뚫리는 식으로 갈라진다.
 * 새 API 가 생겼을 때 여기만 부르면 되도록 모아 둔다.
 *
 * <p><b>없는 주문과 권한 없는 주문을 같은 404 로 응답한다.</b> 403 을 내면 호출자가 상태코드
 * 차이만으로 "그 ID 의 주문이 존재한다"는 사실을 알아낼 수 있다. 주문 ID 는 UUID 라 찍어서
 * 맞히긴 어렵지만, 어디선가 흘러나온 ID 의 실재 여부를 확인해 주는 창구가 되면 안 된다.
 *
 * <p>다만 <b>쓰기</b>는 403 으로 응답한다. 자기 주문이 아닌 것을 고치려는 요청은 이미 그 주문의
 * 존재를 아는 상태이고, 404 를 주면 "주문이 사라졌다"로 오해해 다시 만들려 든다.
 */
@Slf4j
@Component
public class OrderAccessPolicy {

	/**
	 * 조회할 수 있는지 본다.
	 *
	 * @throws BusinessException 권한이 없으면 {@code ORDER_NOT_FOUND} (존재를 숨긴다)
	 */
	public void validateReadable(Order order, JwtPrincipal principal) {
		if (isAllowed(order, principal)) {
			return;
		}

		log.warn("[인가] 권한 없는 주문 조회 차단 : orderId={} callerId={} role={}",
				order.getId(), principal.userId(), principal.role());
		throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
	}

	/**
	 * 바꿀 수 있는지 본다.
	 *
	 * @throws BusinessException 권한이 없으면 {@code AUTH_FORBIDDEN}
	 */
	public void validateWritable(Order order, JwtPrincipal principal) {
		if (isAllowed(order, principal)) {
			return;
		}

		log.warn("[인가] 권한 없는 주문 변경 차단 : orderId={} callerId={} role={}",
				order.getId(), principal.userId(), principal.role());
		throw new BusinessException(ErrorCode.AUTH_FORBIDDEN);
	}

	/** 남의 주문까지 볼 수 있는가. 검색 조건을 좁힐지 판단하는 데 쓴다 */
	public boolean canSeeAllOrders(JwtPrincipal principal) {
		return principal == null || principal.role() == Role.MASTER;
	}

	/**
	 * <p>인증 주체가 없으면 통과시킨다. {@code /api/v1/**} 는 인증이 걸려 있어 평소엔 항상 있고,
	 * 인증을 끄고 띄우는 로컬·스모크 실행에서만 {@code null} 이다.
	 *
	 * <p>지금 판별할 수 있는 것은 <b>수령인 본인인지</b>와 <b>MASTER 인지</b> 둘뿐이다.
	 * 토큰에 {@code userId} 와 {@code role} 만 실려 있어 담당 허브·담당 업체를 알 수 없고,
	 * 그래서 HUB_MANAGER 의 "내 허브 주문만", COMPANY_MANAGER 의 "우리 업체 주문만" 은
	 * 아직 구현할 수 없다. delivery-service 도 같은 이유로 후속 과제로 두었다.
	 */
	private boolean isAllowed(Order order, JwtPrincipal principal) {
		if (principal == null) {
			return true;
		}

		if (principal.role() == Role.MASTER) {
			return true;
		}

		return order.getReceiverUserId().equals(principal.userId());
	}
}
