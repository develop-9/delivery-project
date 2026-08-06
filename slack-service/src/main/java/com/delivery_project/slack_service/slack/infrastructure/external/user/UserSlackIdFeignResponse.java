package com.delivery_project.slack_service.slack.infrastructure.external.user;

import com.delivery_project.slack_service.slack.application.result.UserSlackIdResult;

import java.util.UUID;

public record UserSlackIdFeignResponse(
        UUID userId,
        String slackId
) {

    public UserSlackIdResult toResult() {
        return new UserSlackIdResult(
                userId,
                slackId
        );
    }
}