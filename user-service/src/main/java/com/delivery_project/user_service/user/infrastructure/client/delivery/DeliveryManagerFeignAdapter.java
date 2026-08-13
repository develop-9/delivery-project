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
		} catch (FeignException.Conflict e) {
			// 진행 중인 배송에 배정된 상태 — 서비스 장애가 아니라 삭제를 막아야 하는 정상적인
			// 비즈니스 규칙 위반이므로, DELIVERY_SERVICE_UNAVAILABLE(503)과 구분해서 던진다.
			log.info("[User] 진행 중인 배송이 있어 배송담당자 삭제 불가 userId={}", userId);
			throw new BusinessException(ErrorCode.DELIVERY_MANAGER_HAS_ACTIVE_DELIVERY);
		} catch (FeignException e) {
			log.warn("[User] Delivery Service 연동 실패 userId={}", userId, e);
			throw new BusinessException(ErrorCode.DELIVERY_SERVICE_UNAVAILABLE);
		}
	}

	@Override
	public void deactivate(UUID userId) {
		try {
			deliveryManagerClient.deactivateByUserId(userId);
		} catch (FeignException.NotFound e) {
			// 배송담당자 레코드가 없음 — 정지를 막을 이유가 없어 멱등 성공으로 취급.
			log.info("[User] 연동할 배송담당자 레코드 없음 userId={}", userId);
		} catch (FeignException e) {
			log.warn("[User] Delivery Service 연동 실패 userId={}", userId, e);
			throw new BusinessException(ErrorCode.DELIVERY_SERVICE_UNAVAILABLE);
		}
	}

	@Override
	public void reactivate(UUID userId) {
		try {
			deliveryManagerClient.reactivateByUserId(userId);
		} catch (FeignException.NotFound e) {
			// 배송담당자 레코드가 없음 — 정지 해제를 막을 이유가 없어 멱등 성공으로 취급.
			log.info("[User] 연동할 배송담당자 레코드 없음 userId={}", userId);
		} catch (FeignException e) {
			log.warn("[User] Delivery Service 연동 실패 userId={}", userId, e);
			throw new BusinessException(ErrorCode.DELIVERY_SERVICE_UNAVAILABLE);
		}
	}
}
