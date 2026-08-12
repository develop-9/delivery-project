package com.delivery_project.slack_service.ai_history.application.result;

import java.util.List;
import java.util.UUID;

public record HubBatchResult(
        List<HubResult> hubs,
        List<UUID> notFoundHubIds
) {

    public record HubResult(
            UUID hubId,
            String name,
            String address
    ) {
    }
}