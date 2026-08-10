package com.delivery_project.user_service.user.infrastructure.client.hub;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.user.application.port.HubPort;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubFeignAdapter implements HubPort {

	private final HubClient hubClient;

	@Override
	public void validateExists(UUID hubId) {
		try {
			hubClient.getHub(hubId);
		} catch (FeignException.NotFound e) {
			log.info("[User] 존재하지 않는 허브 hubId={}", hubId);
			throw new BusinessException(ErrorCode.HUB_NOT_FOUND);
		} catch (FeignException e) {
			log.warn("[User] Hub Service 연동 실패 hubId={}", hubId, e);
			throw new BusinessException(ErrorCode.HUB_SERVICE_UNAVAILABLE);
		}
	}
}
