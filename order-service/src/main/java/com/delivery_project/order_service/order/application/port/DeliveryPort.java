package com.delivery_project.order_service.order.application.port;

import java.util.UUID;

/**
 * 배송 생성·취소 포트.
 *
 * <p>주문이 접수되면 배송이 만들어져야 하고, 주문이 취소되면 배송도 취소돼야 한다.
 * 두 호출 모두 <b>주문 트랜잭션 밖</b>에서 일어난다 — DB 커넥션을 쥔 채 네트워크를 기다리면
 * 커넥션 풀이 마른다.
 */
public interface DeliveryPort {

	/**
	 * 배송을 만든다.
	 *
	 * @param departureHubId 선점한 재고가 있는 허브. 배송 출발지
	 * @return 생성된 배송 ID
	 */
	UUID createDelivery(UUID orderId,
			UUID departureHubId,
			UUID destinationHubId,
			String deliveryAddress,
			UUID receiverUserId);

	/**
	 * 배송을 취소한다.
	 *
	 * <p>배송이 없는 주문(생성 실패 후 취소 등)도 취소될 수 있어, 대상이 없으면
	 * 예외를 던지지 않고 조용히 넘어간다.
	 */
	void cancelDelivery(UUID orderId);
}
