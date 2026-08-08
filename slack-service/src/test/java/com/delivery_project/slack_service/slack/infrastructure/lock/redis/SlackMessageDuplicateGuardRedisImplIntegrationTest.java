package com.delivery_project.slack_service.slack.infrastructure.lock.redis;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * docker-compose-infra.yaml의 실제 Redis 컨테이너(localhost:6379)에 붙는 통합 테스트.
 * Redis가 떠 있지 않으면 실패한다.
 * Mock 기반 검증은 SlackMessageDuplicateGuardRedisImplTest가 담당한다.
 */
class SlackMessageDuplicateGuardRedisImplIntegrationTest {

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;
    private static SlackMessageDuplicateGuardRedisImpl duplicateGuard;

    @BeforeAll
    static void setUpRedis() {
        connectionFactory = new LettuceConnectionFactory("localhost", 6379);
        connectionFactory.afterPropertiesSet();

        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();

        duplicateGuard = new SlackMessageDuplicateGuardRedisImpl(redisTemplate);
    }

    @AfterAll
    static void tearDownRedis() {
        connectionFactory.destroy();
    }

    @Test
    void 처음_선점하는_메시지는_lockToken을_반환하고_실제_Redis에_키가_생긴다() {
        // given
        UUID slackMessageId = UUID.randomUUID();

        // when
        String lockToken = duplicateGuard.tryAcquire(slackMessageId);

        // then
        assertThat(lockToken).isNotNull();
        assertThat(redisTemplate.hasKey("slack:processing:" + slackMessageId))
                .isTrue();

        assertThat(
                redisTemplate.opsForValue()
                        .get("slack:processing:" + slackMessageId)
        ).isEqualTo(lockToken);
    }

    @Test
    void 이미_선점된_메시지는_다시_선점할_수_없다() {
        // given
        UUID slackMessageId = UUID.randomUUID();
        String firstLockToken = duplicateGuard.tryAcquire(slackMessageId);

        // when
        String secondLockToken = duplicateGuard.tryAcquire(slackMessageId);

        // then
        assertThat(firstLockToken).isNotNull();
        assertThat(secondLockToken).isNull();
    }

    @Test
    void 본인이_획득한_lockToken으로_release하면_다시_선점할_수_있다() {
        // given
        UUID slackMessageId = UUID.randomUUID();
        String lockToken = duplicateGuard.tryAcquire(slackMessageId);

        // when
        duplicateGuard.release(slackMessageId, lockToken);

        // then
        assertThat(duplicateGuard.tryAcquire(slackMessageId))
                .isNotNull();
    }

    @Test
    void 다른_lockToken으로_release하면_기존_Lock은_삭제되지_않는다() {
        // given
        UUID slackMessageId = UUID.randomUUID();

        String ownerLockToken =
                duplicateGuard.tryAcquire(slackMessageId);

        String otherLockToken =
                UUID.randomUUID().toString();

        // when
        duplicateGuard.release(
                slackMessageId,
                otherLockToken
        );

        // then
        assertThat(
                redisTemplate.opsForValue()
                        .get("slack:processing:" + slackMessageId)
        ).isEqualTo(ownerLockToken);
    }

    @Test
    void 선점된_키에는_TTL이_걸려_영구히_남지_않는다() {
        // given
        UUID slackMessageId = UUID.randomUUID();

        // when
        duplicateGuard.tryAcquire(slackMessageId);

        // then
        Long ttlSeconds =
                redisTemplate.getExpire(
                        "slack:processing:" + slackMessageId
                );

        assertThat(ttlSeconds).isPositive();
    }
}