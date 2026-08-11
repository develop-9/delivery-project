package com.delivery_project.delivery_service.delivery.infrastructure.adapter;

import com.delivery_project.delivery_service.delivery.application.port.HubPort;
import com.delivery_project.delivery_service.delivery.infrastructure.client.HubInternalClient;
import com.delivery_project.delivery_service.global.exception.BusinessException;
import com.delivery_project.delivery_service.global.exception.ErrorCode;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HubFeignAdapter implements HubPort {

    private final HubInternalClient hubInternalClient;

    @Override
    public void validateHubExists(UUID hubId) {
        try {
            hubInternalClient.getHub(hubId);

        } catch (FeignException.NotFound e) {
            throw new BusinessException(
                    ErrorCode.HUB_NOT_FOUND
            );

        } catch (FeignException e) {
            throw new BusinessException(
                    ErrorCode.HUB_SERVICE_UNAVAILABLE
            );
        }
    }
}