package com.delivery_project.slack_service.slack.infrastructure.lock.redis;

import com.delivery_project.slack_service.slack.application.port.SlackMessageDuplicateGuard;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Component
public class SlackMessageDuplicateGuardRedisImpl implements SlackMessageDuplicateGuard {

    private static final String KEY_PREFIX = "slack:processing:";

    // Lock이 영구적으로 남는 것을 방지하기 위한 TTL.
// 메시지 처리 시간이 TTL을 초과하면 Lock이 만료될 수 있으므로,
// 처리 시간보다 충분히 긴 값으로 설정해야 한다.
    private static final Duration LOCK_TTL = Duration.ofSeconds(30);

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    if redis.call('get', KEYS[1]) == ARGV[1] then
                        return redis.call('del', KEYS[1])
                    else
                        return 0
                    end
                    """,
                    Long.class
            );

    private final StringRedisTemplate redisTemplate;

    public SlackMessageDuplicateGuardRedisImpl(
            StringRedisTemplate redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public String tryAcquire(UUID slackMessageId) {
        String lockToken = UUID.randomUUID().toString();

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(
                        key(slackMessageId),
                        lockToken,
                        LOCK_TTL
                );

        if (Boolean.TRUE.equals(acquired)) {
            return lockToken;
        }

        return null;
    }

    @Override
    public void release(UUID slackMessageId, String lockToken) {
        redisTemplate.execute(
                RELEASE_SCRIPT,
                Collections.singletonList(key(slackMessageId)),
                lockToken
        );
    }

    private String key(UUID slackMessageId) {
        return KEY_PREFIX + slackMessageId;
    }
}