package com.delivery_project.user_service.user.application.port;

import java.util.UUID;

/**
 * 이미 삭제됐거나 존재하지 않는 배송담당자를 지우는 호출은 성공으로 취급한다(멱등).
 * 진행 중인 배송에 배정된 상태면 BusinessException(DELIVERY_MANAGER_HAS_ACTIVE_DELIVERY, 409)이,
 * 그 외의 실패는 BusinessException(DELIVERY_SERVICE_UNAVAILABLE, 503)이 던져진다.
 *
 * deactivate/reactivate는 대상 배송담당자 레코드가 없어도 성공으로 취급한다(멱등) — 정지/정지
 * 해제는 delete()와 달리 막아야 할 비즈니스 규칙이 없어(진행 중인 배송이 있어도 그 배송은 그대로
 * 마치고 신규 배정만 막는 방식이라 Delivery Service가 자체적으로 허용한다) Conflict 케이스가
 * 없고, 그 외 실패는 마찬가지로 BusinessException(DELIVERY_SERVICE_UNAVAILABLE, 503)이 던져진다.
 */
public interface DeliveryManagerPort {

	void deleteByUserId(UUID userId);

	void deactivate(UUID userId);

	void reactivate(UUID userId);
}
