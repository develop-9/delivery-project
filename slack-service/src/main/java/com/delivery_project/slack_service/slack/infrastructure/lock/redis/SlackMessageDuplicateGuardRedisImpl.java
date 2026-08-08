package com.delivery_project.slack_service.slack.infrastructure.lock.redis;

import com.delivery_project.slack_service.slack.application.port.SlackMessageDuplicateGuard;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class SlackMessageDuplicateGuardRedisImpl implements SlackMessageDuplicateGuard {

    private static final String KEY_PREFIX = "slack:processing:";

    // Consumer가 정상 종료되면 release()로 즉시 해제되므로,
    // 이 TTL은 consumer가 비정상 종료되었을 때를 대비한 안전장치일 뿐이다.
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    private final StringRedisTemplate redisTemplate;

    public SlackMessageDuplicateGuardRedisImpl(
            StringRedisTemplate redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryAcquire(UUID slackMessageId) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key(slackMessageId), "1", LOCK_TTL);

        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void release(UUID slackMessageId) {
        redisTemplate.delete(key(slackMessageId));
    }

    private String key(UUID slackMessageId) {
        return KEY_PREFIX + slackMessageId;
    }
}
