package com.delivery_project.slack_service.slack.infrastructure.lock.redis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    void 선점되지_않은_키는_tryAcquire가_true를_반환한다() {
        // given
        slackMessageDuplicateGuard = new SlackMessageDuplicateGuardRedisImpl(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(true);

        // when
        boolean acquired = slackMessageDuplicateGuard.tryAcquire(slackMessageId);

        // then
        assertThat(acquired).isTrue();
        verify(valueOperations)
                .setIfAbsent(eq("slack:processing:" + slackMessageId), anyString(), any(Duration.class));
    }

    @Test
    void 이미_선점된_키는_tryAcquire가_false를_반환한다() {
        // given
        slackMessageDuplicateGuard = new SlackMessageDuplicateGuardRedisImpl(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(false);

        // when
        boolean acquired = slackMessageDuplicateGuard.tryAcquire(slackMessageId);

        // then
        assertThat(acquired).isFalse();
    }

    @Test
    void Redis_응답이_null이면_중복으로_간주해_tryAcquire가_false를_반환한다() {
        // given
        slackMessageDuplicateGuard = new SlackMessageDuplicateGuardRedisImpl(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(null);

        // when
        boolean acquired = slackMessageDuplicateGuard.tryAcquire(slackMessageId);

        // then
        assertThat(acquired).isFalse();
    }

    @Test
    void release는_해당_메시지의_키를_삭제한다() {
        // given
        slackMessageDuplicateGuard = new SlackMessageDuplicateGuardRedisImpl(redisTemplate);

        // when
        slackMessageDuplicateGuard.release(slackMessageId);

        // then
        verify(redisTemplate).delete("slack:processing:" + slackMessageId);
    }
}
