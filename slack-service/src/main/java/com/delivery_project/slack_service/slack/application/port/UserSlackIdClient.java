package com.delivery_project.slack_service.slack.application.port;

import com.delivery_project.slack_service.slack.application.result.UserSlackIdResult;

import java.util.UUID;

public interface UserSlackIdClient {

    UserSlackIdResult getUserSlackId(UUID userId);
}