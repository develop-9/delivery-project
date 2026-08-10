package com.delivery_project.order_service.order.application.port;

import java.util.UUID;

/**
 * 같은 주문 요청이 두 번 처리되지 않게 막는 포트.
 *
 * <p>주문 생성은 재고 선점과 배송 생성을 함께 일으켜서, 중복 처리되면 재고가 두 배로 잠기고
 * 배송도 두 건 만들어진다. 사용자가 버튼을 두 번 누르거나 클라이언트가 응답을 못 받아
 * 재요청하는 것만으로 이 상태가 된다.
 *
 * <p>선점은 <b>원자적</b>이어야 한다. "값이 있는지 보고 없으면 넣는다"를 두 번의 호출로 하면
 * 그 사이에 다른 요청이 끼어든다.
 */
public interface IdempotencyPort {

	/**
	 * 이 키로 작업을 시작할 수 있는지 확인하고, 가능하면 선점한다.
	 *
	 * @return 시작 가능 여부와 이미 만들어진 주문
	 */
	Reservation begin(String key);

	/** 작업이 끝났다. 이후 같은 키로 오는 요청은 만들어진 주문을 그대로 돌려받는다 */
	void complete(String key, UUID orderId);

	/** 작업이 실패했다. 선점을 풀어 같은 키로 다시 시도할 수 있게 한다 */
	void release(String key);

	/**
	 * @param acquired         이번 요청이 선점에 성공했는가
	 * @param completedOrderId 이미 완료된 주문. {@code acquired == false} 이고 이 값도 {@code null}
	 *                         이면 다른 요청이 아직 처리 중이라는 뜻이다
	 */
	record Reservation(boolean acquired, UUID completedOrderId) {

		public static Reservation started() {
			return new Reservation(true, null);
		}

		public static Reservation inProgress() {
			return new Reservation(false, null);
		}

		public static Reservation completed(UUID orderId) {
			return new Reservation(false, orderId);
		}

		public boolean isInProgress() {
			return !acquired && completedOrderId == null;
		}
	}
}
