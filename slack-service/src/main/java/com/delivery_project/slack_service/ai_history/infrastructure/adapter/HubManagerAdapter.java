package com.delivery_project.slack_service.ai_history.infrastructure.adapter;

import com.delivery_project.slack_service.ai_history.application.port.HubManagerPort;
import com.delivery_project.slack_service.ai_history.application.result.HubManagerResult;
import com.delivery_project.slack_service.ai_history.infrastructure.client.user.HubManagerFeignClient;
import com.delivery_project.slack_service.global.exception.BusinessException;
import com.delivery_project.slack_service.global.exception.ErrorCode;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HubManagerAdapter implements HubManagerPort {

    private static final String HUB_MANAGER_ROLE =
            "HUB_MANAGER";

    private final HubManagerFeignClient hubManagerFeignClient;

    @Override
    public HubManagerResult getHubManager(UUID hubId) {
        HubManagerFeignClient.UserApiResponse response;

        try {
            response =
                    hubManagerFeignClient.getHubManager(
                            hubId,
                            HUB_MANAGER_ROLE
                    );
        } catch (FeignException.NotFound exception) {
            throw new BusinessException(
                    ErrorCode.DEPENDENCY_SERVICE_UNAVAILABLE
            );
        } catch (FeignException exception) {
            throw new BusinessException(
                    ErrorCode.DEPENDENCY_SERVICE_UNAVAILABLE
            );
        }

        if (
                response == null
                        || !response.success()
                        || response.data() == null
        ) {
            throw new BusinessException(
                    ErrorCode.DEPENDENCY_SERVICE_UNAVAILABLE
            );
        }

        HubManagerFeignClient.UserData data =
                response.data();

        return new HubManagerResult(
                data.userId(),
                data.name(),
                data.role(),
                data.hubId()
        );
    }
}