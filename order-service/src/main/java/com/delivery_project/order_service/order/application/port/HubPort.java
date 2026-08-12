package com.delivery_project.order_service.order.application.port;

import java.util.List;
import java.util.UUID;

/**
 * 허브 목록 조회 포트.
 *
 * <p>상품이 등록되면 <b>모든 허브에 재고 행을 하나씩</b> 만든다(8/4 회의 결정).
 * 허브 목록은 hub-service 소유라 order 가 들고 있을 수 없다.
 */
public interface HubPort {

	/**
	 * 살아 있는 허브 ID 전체.
	 *
	 * @return 허브가 하나도 없으면 빈 목록. 오류가 아니다
	 */
	List<UUID> getAllHubIds();
}
