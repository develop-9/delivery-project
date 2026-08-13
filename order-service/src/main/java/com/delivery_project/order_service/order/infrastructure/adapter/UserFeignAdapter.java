package com.delivery_project.order_service.order.infrastructure.adapter;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import com.delivery_project.order_service.order.application.port.UserPort;
import com.delivery_project.order_service.order.infrastructure.client.UserInternalClient;
import com.delivery_project.order_service.order.infrastructure.client.dto.InternalApiResponse;
import com.delivery_project.order_service.order.infrastructure.client.dto.UserInfoResponse;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserFeignAdapter implements UserPort {

	private final UserInternalClient userInternalClient;

	@Override
	public Receiver getReceiver(UUID userId) {
		try {
			InternalApiResponse<UserInfoResponse> response = userInternalClient.getUser(userId);

			if (response == null || !response.success() || response.data() == null) {
				throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
						"사용자 정보를 가져올 수 없습니다.");
			}

			UserInfoResponse user = response.data();
			return new Receiver(user.userId(), user.name(), user.hubId(), user.companyId());

		} catch (FeignException.NotFound exception) {
			log.warn("[사용자] 존재하지 않는 수령인 : [{}]", userId);
			throw new BusinessException(ErrorCode.USER_NOT_FOUND);

		} catch (FeignException exception) {
			log.error("[사용자] 조회 실패 : [{}] status={}", userId, exception.status(), exception);
			throw new BusinessException(ErrorCode.EXTERNAL_SERVICE_UNAVAILABLE,
					"사용자 서비스를 사용할 수 없습니다.");
		}
	}
}
