package com.delivery_project.slack_service.ai_history.application.port;

import com.delivery_project.slack_service.ai_history.application.result.HubBatchResult;

import java.util.List;
import java.util.UUID;

public interface HubPort {

    HubBatchResult getHubs(List<UUID> hubIds);
}