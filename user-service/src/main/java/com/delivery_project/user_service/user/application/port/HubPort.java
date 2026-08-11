package com.delivery_project.user_service.user.application.port;

import java.util.UUID;

/**
 * 존재하지 않는 허브면 BusinessException(HUB_NOT_FOUND, 404)이,
 * Hub Service 연동 실패면 BusinessException(HUB_SERVICE_UNAVAILABLE, 503)이 던져진다.
 */
public interface HubPort {

	void validateExists(UUID hubId);
}
