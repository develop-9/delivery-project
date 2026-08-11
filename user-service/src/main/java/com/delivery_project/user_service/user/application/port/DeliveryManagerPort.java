package com.delivery_project.user_service.user.application.port;

import java.util.UUID;

/**
 * 이미 삭제됐거나 존재하지 않는 배송담당자를 지우는 호출은 성공으로 취급한다(멱등).
 * 진행 중인 배송에 배정된 상태면 BusinessException(DELIVERY_MANAGER_HAS_ACTIVE_DELIVERY, 409)이,
 * 그 외의 실패는 BusinessException(DELIVERY_SERVICE_UNAVAILABLE, 503)이 던져진다.
 */
public interface DeliveryManagerPort {

	void deleteByUserId(UUID userId);
}
