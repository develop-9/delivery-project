package com.delivery_project.slack_service.slack.infrastructure.lock.redis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SlackMessageDuplicateGuardRedisImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private SlackMessageDuplicateGuardRedisImpl slackMessageDuplicateGuard;

    private final UUID slackMessageId = UUID.randomUUID();

    @Test
    void 선점되지_않은_키는_tryAcquire가_lockToken을_반환한다() {
        // given
        slackMessageDuplicateGuard =
                new SlackMessageDuplicateGuardRedisImpl(redisTemplate);

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        when(valueOperations.setIfAbsent(
                anyString(),
                anyString(),
                any(Duration.class)
        )).thenReturn(true);

        // when
        String lockToken =
                slackMessageDuplicateGuard.tryAcquire(slackMessageId);

        // then
        assertThat(lockToken).isNotNull();

        verify(valueOperations)
                .setIfAbsent(
                        eq("slack:processing:" + slackMessageId),
                        eq(lockToken),
                        any(Duration.class)
                );
    }

    @Test
    void 이미_선점된_키는_tryAcquire가_null을_반환한다() {
        // given
        slackMessageDuplicateGuard =
                new SlackMessageDuplicateGuardRedisImpl(redisTemplate);

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        when(valueOperations.setIfAbsent(
                anyString(),
                anyString(),
                any(Duration.class)
        )).thenReturn(false);

        // when
        String lockToken =
                slackMessageDuplicateGuard.tryAcquire(slackMessageId);

        // then
        assertThat(lockToken).isNull();
    }

    @Test
    void Redis_응답이_null이면_tryAcquire도_null을_반환한다() {
        // given
        slackMessageDuplicateGuard =
                new SlackMessageDuplicateGuardRedisImpl(redisTemplate);

        when(redisTemplate.opsForValue())
                .thenReturn(valueOperations);

        when(valueOperations.setIfAbsent(
                anyString(),
                anyString(),
                any(Duration.class)
        )).thenReturn(null);

        // when
        String lockToken =
                slackMessageDuplicateGuard.tryAcquire(slackMessageId);

        // then
        assertThat(lockToken).isNull();
    }

    @Test
    void release는_lockToken과_함께_Lua_Script를_실행한다() {
        // given
        slackMessageDuplicateGuard =
                new SlackMessageDuplicateGuardRedisImpl(redisTemplate);

        String lockToken = UUID.randomUUID().toString();

        // when
        slackMessageDuplicateGuard.release(
                slackMessageId,
                lockToken
        );

        // then
        verify(redisTemplate).execute(
                any(RedisScript.class),
                eq(List.of("slack:processing:" + slackMessageId)),
                eq(lockToken)
        );
    }
}