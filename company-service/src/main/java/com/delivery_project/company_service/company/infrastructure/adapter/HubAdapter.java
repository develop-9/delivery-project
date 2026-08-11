package com.delivery_project.company_service.company.infrastructure.adapter;

import com.delivery_project.company_service.company.application.port.HubPort;
import com.delivery_project.company_service.company.application.port.dto.HubInfo;
import com.delivery_project.company_service.company.infrastructure.client.dto.response.HubFeignResponse;
import com.delivery_project.company_service.company.infrastructure.client.hub.HubClient;
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
public class HubAdapter implements HubPort {

    private final HubClient hubClient;

    @Override
    public HubInfo getHub(UUID hubId) {

        try {
            HubFeignResponse hubFeignResponse = hubClient.getHub(hubId).data();

            return new HubInfo(
                    hubFeignResponse.hubId()
            );

        } catch (FeignException.NotFound e) {
            log.warn(
                    "Hub 조회 실패. 존재하지 않는 허브. hubId={}",
                    hubId
            );

            throw new BusinessException(
                    ErrorCode.HUB_NOT_FOUND
            );
        } catch (FeignException e) {
            log.error(
                    "Hub Service 호출 실패. hubId={}, status={}",
                    hubId,
                    e.status(),
                    e
            );

            throw new BusinessException(
                    ErrorCode.HUB_SERVICE_UNAVAILABLE
            );
        }
    }
}
