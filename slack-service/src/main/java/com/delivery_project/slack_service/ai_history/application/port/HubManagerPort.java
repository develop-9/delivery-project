package com.delivery_project.slack_service.ai_history.application.port;

import com.delivery_project.slack_service.ai_history.application.result.HubManagerResult;

import java.util.UUID;

public interface HubManagerPort {

    HubManagerResult getHubManager(UUID hubId);
}