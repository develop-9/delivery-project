package com.delivery_project.order_service.order.infrastructure.adapter;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.order.application.port.HubPort;
import com.delivery_project.order_service.order.infrastructure.client.HubInternalClient;
import com.delivery_project.order_service.order.infrastructure.client.dto.HubIdsResponse;
import com.delivery_project.order_service.order.infrastructure.client.dto.InternalApiResponse;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubFeignAdapter implements HubPort {

	private final HubInternalClient hubInternalClient;

	@Override
	public List<UUID> getAllHubIds() {
		try {
			InternalApiResponse<HubIdsResponse> response = hubInternalClient.getAllHubIds();

			if (response == null || !response.success() || response.data() == null) {
				throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
						"허브 목록을 가져올 수 없습니다.");
			}

			// 허브 0건은 hub-service 가 정상 응답한 결과다. 여기서 오류로 바꾸지 않는다
			return response.data().hubIds();

		} catch (FeignException exception) {
			log.error("[허브] 목록 조회 실패 : status={}", exception.status(), exception);
			throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
					"허브 서비스를 사용할 수 없습니다.");
		}
	}
}
