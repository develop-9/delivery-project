package com.delivery_project.order_service.order.application.port;

import java.util.UUID;

/**
 * 사용자 조회 포트. 주문 수령인이 실존하는지 확인하고 소속 업체를 얻는다.
 *
 * <p>토큰에는 {@code userId} 와 {@code role} 만 들어 있어 소속 업체를 알 수 없다.
 * 담당 업체 기준 권한 검증을 하려면 user-service 에 물어보는 수밖에 없다.
 */
public interface UserPort {

	Receiver getReceiver(UUID userId);

	/**
	 * @param companyId 소속 업체. MASTER · HUB_MANAGER 처럼 업체가 없는 역할은 {@code null}
	 */
	record Receiver(UUID userId, String name, UUID hubId, UUID companyId) {
	}
}
