package com.delivery_project.user_service.user.infrastructure.client.delivery;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.delivery_project.user_service.global.exception.BusinessException;
import com.delivery_project.user_service.global.exception.ErrorCode;
import com.delivery_project.user_service.user.application.port.DeliveryManagerPort;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryManagerFeignAdapter implements DeliveryManagerPort {

	private final DeliveryManagerClient deliveryManagerClient;

	@Override
	public void deleteByUserId(UUID userId) {
		try {
			deliveryManagerClient.deleteByUserId(userId);
		} catch (FeignException.NotFound e) {
			// 배송담당자 레코드가 없거나 이미 삭제됨 — 멱등 삭제로 취급.
			log.info("[User] 연동할 배송담당자 레코드 없음 userId={}", userId);
		} catch (FeignException e) {
			log.warn("[User] Delivery Service 연동 실패 userId={}", userId, e);
			throw new BusinessException(ErrorCode.DELIVERY_SERVICE_UNAVAILABLE);
		}
	}
}
