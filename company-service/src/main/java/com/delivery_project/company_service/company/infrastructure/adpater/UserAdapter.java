package com.delivery_project.company_service.company.infrastructure.adpater;

import com.delivery_project.company_service.company.application.port.UserPort;
import com.delivery_project.company_service.company.application.port.dto.CallerInfo;
import com.delivery_project.company_service.company.infrastructure.client.dto.UserFeignResponse;
import com.delivery_project.company_service.company.infrastructure.client.user.UserClient;
import com.delivery_project.company_service.global.exception.BusinessException;
import com.delivery_project.company_service.global.exception.ErrorCode;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserAdapter implements UserPort {

    private final UserClient userClient;

    @Override
    public CallerInfo getCaller(UUID callerId) {

        try {
            UserFeignResponse response =
                    userClient.getCaller(callerId).data();

            return new CallerInfo(
                    response.userId(),
                    response.role(),
                    response.hubId(),
                    response.companyId()
            );

        } catch (FeignException.NotFound e) {
            log.warn(
                    "User 조회 실패. 존재하지 않는 사용자. callerId={}",
                    callerId
            );

            throw new BusinessException(
                    ErrorCode.USER_NOT_FOUND
            );

        } catch (FeignException e) {
            log.error(
                    "User Service 호출 실패. callerId={}, status={}",
                    callerId,
                    e.status(),
                    e
            );

            throw new BusinessException(
                    ErrorCode.USER_SERVICE_UNAVAILABLE
            );
        }
    }
}
