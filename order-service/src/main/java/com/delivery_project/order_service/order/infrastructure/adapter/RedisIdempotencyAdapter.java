package com.delivery_project.order_service.order.infrastructure.adapter;

import com.delivery_project.order_service.order.application.port.IdempotencyPort;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Redis {@code SET key value NX EX} 로 중복 요청을 막는다.
 *
 * <p>{@code setIfAbsent} 한 번이 "없으면 넣기"를 원자적으로 처리한다. 조회 후 저장으로 나누면
 * 그 사이에 다른 요청이 끼어들어 둘 다 통과한다.
 *
 * <p>DB 유니크 제약이 아니라 Redis 를 쓰는 이유는, 막아야 하는 것이 <b>주문 행의 중복</b>이 아니라
 * <b>처리 과정 전체의 중복</b>이기 때문이다. 주문 행이 만들어지기 전에 이미 재고 선점과 배송 생성이
 * 시작되므로 DB 제약으로는 늦다.
 *
 * <p>Redis 가 죽으면 이 방어는 사라지지만 주문 자체는 계속 받는다. 중복을 막자고 주문을 통째로
 * 못 받게 하는 것이 더 큰 손해라고 봤다 (fail-open).
 */
@Slf4j
@Component
public class RedisIdempotencyAdapter implements IdempotencyPort {

	private static final String KEY_PREFIX = "order:idempotency:";
	private static final String IN_PROGRESS = "IN_PROGRESS";

	private final StringRedisTemplate redisTemplate;

	/** 처리 중 표시가 남는 시간. 서버가 죽어 release 를 못 해도 이 시간이 지나면 풀린다 */
	private final Duration inProgressTtl;

	/** 완료 기록이 남는 시간. 이 시간 안의 재요청은 같은 주문을 돌려받는다 */
	private final Duration completedTtl;

	public RedisIdempotencyAdapter(
			StringRedisTemplate redisTemplate,
			@Value("${order.idempotency.in-progress-ttl-seconds:300}") long inProgressTtlSeconds,
			@Value("${order.idempotency.completed-ttl-hours:24}") long completedTtlHours) {
		this.redisTemplate = redisTemplate;
		this.inProgressTtl = Duration.ofSeconds(inProgressTtlSeconds);
		this.completedTtl = Duration.ofHours(completedTtlHours);
	}

	@Override
	public Reservation begin(String key) {
		String redisKey = redisKey(key);

		if (tryOccupy(redisKey)) {
			return Reservation.started();
		}

		String value = redisTemplate.opsForValue().get(redisKey);

		if (value == null) {
			// 방금 TTL 이 만료됐다. 한 번 더 선점을 시도하고, 그것도 실패하면 다른 요청에 양보한다
			return tryOccupy(redisKey) ? Reservation.started() : Reservation.inProgress();
		}

		if (IN_PROGRESS.equals(value)) {
			log.warn("[멱등] 아직 처리 중인 요청 : key={}", key);
			return Reservation.inProgress();
		}

		log.info("[멱등] 이미 처리된 요청 : key={} orderId={}", key, value);
		return Reservation.completed(UUID.fromString(value));
	}

	@Override
	public void complete(String key, UUID orderId) {
		redisTemplate.opsForValue().set(redisKey(key), orderId.toString(), completedTtl);
	}

	@Override
	public void release(String key) {
		redisTemplate.delete(redisKey(key));
	}

	private boolean tryOccupy(String redisKey) {
		return Boolean.TRUE.equals(
				redisTemplate.opsForValue().setIfAbsent(redisKey, IN_PROGRESS, inProgressTtl));
	}

	private String redisKey(String key) {
		return KEY_PREFIX + key;
	}
}
