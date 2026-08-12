package com.delivery_project.order_service.order.infrastructure.adapter;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.order.application.port.DeliveryPort;
import com.delivery_project.order_service.order.infrastructure.client.DeliveryInternalClient;
import com.delivery_project.order_service.order.infrastructure.client.dto.DeliveryCreateApiRequest;
import com.delivery_project.order_service.order.infrastructure.client.dto.DeliveryCreateApiResponse;
import com.delivery_project.order_service.order.infrastructure.client.dto.InternalApiResponse;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/*
 * TODO: delivery-service 와 통합 확인
 * - 대상 API: POST /internal/v1/deliveries, PATCH /internal/v1/deliveries/orders/{orderId}/cancel
 * - 확인 항목:
 *   1. departureHubId 로 hub-service 경로 산출이 되는지
 *   2. 주문 취소 시 배송 담당자 상태가 AVAILABLE 로 복원되는지
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeliveryFeignAdapter implements DeliveryPort {

	private final DeliveryInternalClient deliveryInternalClient;

	@Override
	public UUID createDelivery(UUID orderId,
			UUID departureHubId,
			UUID destinationHubId,
			String deliveryAddress,
			UUID receiverUserId) {

		DeliveryCreateApiRequest request = new DeliveryCreateApiRequest(
				orderId, departureHubId, destinationHubId, deliveryAddress, receiverUserId);

		try {
			InternalApiResponse<DeliveryCreateApiResponse> response =
					deliveryInternalClient.createDelivery(request);

			if (response == null || !response.success() || response.data() == null) {
				throw new BusinessException(ErrorCode.DELIVERY_CREATE_FAILED);
			}

			log.info("[배송] 생성 요청 성공 : orderId={} deliveryId={} departureHubId={}",
					orderId, response.data().deliveryId(), departureHubId);

			return response.data().deliveryId();

		} catch (FeignException exception) {
			log.error("[배송] 생성 요청 실패 : orderId={} departureHubId={} status={}",
					orderId, departureHubId, exception.status(), exception);
			throw new BusinessException(ErrorCode.DELIVERY_CREATE_FAILED);
		}
	}

	@Override
	public void cancelDelivery(UUID orderId) {
		try {
			deliveryInternalClient.cancelDelivery(orderId);
			log.info("[배송] 취소 요청 성공 : orderId={}", orderId);

		} catch (FeignException.NotFound exception) {
			// 배송 생성 전에 취소된 주문. 취소할 배송이 없는 것은 오류가 아니다
			log.info("[배송] 취소 대상 없음 : orderId={}", orderId);

		} catch (FeignException exception) {
			log.error("[배송] 취소 요청 실패 : orderId={} status={}",
					orderId, exception.status(), exception);
			throw new BusinessException(ErrorCode.DELIVERY_CANCEL_FAILED);
		}
	}
}
