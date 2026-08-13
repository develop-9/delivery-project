package com.delivery_project.user_service.user.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

@ExtendWith(MockitoExtension.class)
class RedisUserInvalidationWriterTest {

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private RedisUserInvalidationWriter writer;

	@Test
	void write는_user_userId_invalidatedAt_키에_만료시간까지_저장한다() {
		// given
		writer = new RedisUserInvalidationWriter(redisTemplate);
		ReflectionTestUtils.setField(writer, "accessTokenExpirationMillis", 3_600_000L);
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		UUID userId = UUID.randomUUID();
		Instant invalidatedAt = Instant.now();

		// when
		writer.write(userId, invalidatedAt);

		// then
		ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
		verify(valueOperations).set(
				org.mockito.ArgumentMatchers.eq("user:" + userId + ":invalidatedAt"),
				org.mockito.ArgumentMatchers.eq(String.valueOf(invalidatedAt.toEpochMilli())),
				ttlCaptor.capture());
		assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofHours(1));
	}

	/**
	 * writeFallback은 resilience4j AOP가 아니면 직접 호출할 수 없는 private 메서드라 리플렉션으로
	 * 부른다. 서킷이 열렸을 때 CallNotPermittedException을 그대로 다시 던지는지 확인한다 —
	 * 여기서 삼키면 UserInvalidationQueueConsumer의 재시도 로직이 안 타서 아웃박스가 영원히
	 * PENDING에 머무를 수 있다.
	 */
	@Test
	void writeFallback은_예외를_삼키지_않고_그대로_다시_던진다() throws Exception {
		// given
		writer = new RedisUserInvalidationWriter(redisTemplate);
		CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(
				CircuitBreakerConfig.custom().failureRateThreshold(100).build());
		CircuitBreaker circuitBreaker = registry.circuitBreaker("redis");
		circuitBreaker.transitionToOpenState();
		CallNotPermittedException circuitOpenException =
				CallNotPermittedException.createCallNotPermittedException(circuitBreaker);

		Method fallback = RedisUserInvalidationWriter.class.getDeclaredMethod(
				"writeFallback", UUID.class, Instant.class, Throwable.class);
		fallback.setAccessible(true);

		// when & then
		assertThatThrownBy(() -> fallback.invoke(writer, UUID.randomUUID(), Instant.now(), circuitOpenException))
				.hasCauseInstanceOf(CallNotPermittedException.class);
	}
}
