package com.delivery_project.slack_service.slack.application.port;

import java.util.UUID;

public interface SlackMessageDuplicateGuard {

    String tryAcquire(UUID slackMessageId);

    void release(UUID slackMessageId, String lockToken);
}
