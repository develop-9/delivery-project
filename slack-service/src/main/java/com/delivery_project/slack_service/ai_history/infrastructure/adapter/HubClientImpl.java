package com.delivery_project.slack_service.ai_history.infrastructure.external;

import com.delivery_project.slack_service.ai_history.application.port.HubClient;
import com.delivery_project.slack_service.ai_history.application.result.HubBatchResult;
import com.delivery_project.slack_service.global.exception.BusinessException;
import com.delivery_project.slack_service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HubClientImpl implements HubClient {

    private final HubFeignClient hubFeignClient;

    @Override
    public HubBatchResult getHubs(List<UUID> hubIds) {
        try {
            HubFeignClient.HubBatchApiResponse response =
                    hubFeignClient.getHubs(hubIds);

            if (response == null || response.data() == null) {
                throw new BusinessException(
                        ErrorCode.DEPENDENCY_SERVICE_UNAVAILABLE
                );
            }

            List<HubBatchResult.HubResult> hubs =
                    response.data().hubs().stream()
                            .map(hub -> new HubBatchResult.HubResult(
                                    hub.hubId(),
                                    hub.name(),
                                    hub.address()
                            ))
                            .toList();

            return new HubBatchResult(
                    hubs,
                    response.data().notFoundHubIds()
            );

        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(
                    ErrorCode.DEPENDENCY_SERVICE_UNAVAILABLE
            );
        }
    }
}