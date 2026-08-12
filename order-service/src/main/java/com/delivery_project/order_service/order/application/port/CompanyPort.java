package com.delivery_project.order_service.order.application.port;

import java.util.UUID;

/**
 * 업체 정보 조회 포트. 배송 생성에 필요한 <b>수령 업체</b> 값을 가져온다.
 *
 * <p>order 는 업체를 소유하지 않으므로 도착 허브와 주소를 만들어낼 수 없다.
 * 두 값 모두 company-service 의 {@code p_companies} 에 있다.
 */
public interface CompanyPort {

	/**
	 * @param companyId 수령 업체 ID
	 * @return 배송 생성에 필요한 최소 정보
	 */
	ReceiverCompany getReceiverCompany(UUID companyId);

	/**
	 * @param hubId   업체 소속 허브. 배송 도착 허브가 된다
	 * @param address 업체 주소. 최종 배송지가 된다
	 */
	record ReceiverCompany(UUID companyId, UUID hubId, String address) {
	}
}
