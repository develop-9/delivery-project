package com.delivery_project.order_service.order.infrastructure.adapter;

import com.delivery_project.order_service.order.application.port.IdempotencyPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.BDDMockito.then;

/**
 * Redis 기반 중복 요청 차단.
 *
 * <p>키에 접두사가 붙는지, TTL 이 걸리는지, 그리고 <b>선점을 한 번의 원자적 연산으로 하는지</b>를
 * 고정한다. 조회 후 저장으로 나누면 그 사이에 다른 요청이 끼어들어 둘 다 통과한다.
 */
@ExtendWith(MockitoExtension.class)
class RedisIdempotencyAdapterTest {

	private static final String KEY = "user-1:key-1";
	private static final String REDIS_KEY = "order:idempotency:user-1:key-1";

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	private RedisIdempotencyAdapter adapter;

	@BeforeEach
	void setUp() {
		// release() 는 opsForValue 를 쓰지 않아 lenient 로 둔다
		lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		adapter = new RedisIdempotencyAdapter(redisTemplate, 300, 24);
	}

	@Test
	@DisplayName("처음 오는 키는 선점에 성공한다")
	void firstRequestAcquires() {
		// given
		given(valueOperations.setIfAbsent(eq(REDIS_KEY), eq("IN_PROGRESS"), any(Duration.class)))
				.willReturn(true);

		// when
		IdempotencyPort.Reservation reservation = adapter.begin(KEY);

		// then
		assertThat(reservation.acquired()).isTrue();
		// 조회 없이 한 번의 연산으로 끝나야 한다. 조회 후 저장이면 그 사이에 끼어들 수 있다
		then(valueOperations).should().setIfAbsent(eq(REDIS_KEY), eq("IN_PROGRESS"), any(Duration.class));
	}

	@Test
	@DisplayName("앞선 요청이 처리 중이면 선점하지 못한다")
	void inProgressRequestIsBlocked() {
		// given
		given(valueOperations.setIfAbsent(any(), any(), any(Duration.class))).willReturn(false);
		given(valueOperations.get(REDIS_KEY)).willReturn("IN_PROGRESS");

		// when
		IdempotencyPort.Reservation reservation = adapter.begin(KEY);

		// then
		assertThat(reservation.acquired()).isFalse();
		assertThat(reservation.isInProgress()).isTrue();
	}

	@Test
	@DisplayName("이미 끝난 요청이면 그때 만든 주문 ID 를 돌려준다")
	void completedRequestReturnsOrderId() {
		// given
		UUID orderId = UUID.randomUUID();
		given(valueOperations.setIfAbsent(any(), any(), any(Duration.class))).willReturn(false);
		given(valueOperations.get(REDIS_KEY)).willReturn(orderId.toString());

		// when
		IdempotencyPort.Reservation reservation = adapter.begin(KEY);

		// then
		assertThat(reservation.completedOrderId()).isEqualTo(orderId);
		assertThat(reservation.isInProgress()).isFalse();
	}

	@Test
	@DisplayName("선점 직후 TTL 이 만료돼 값이 사라지면 한 번 더 시도한다")
	void expiredBetweenCallsRetriesOnce() {
		// given — setIfAbsent 는 실패했는데 값을 읽으니 없다(그 사이 만료)
		given(valueOperations.setIfAbsent(any(), any(), any(Duration.class)))
				.willReturn(false)
				.willReturn(true);
		given(valueOperations.get(REDIS_KEY)).willReturn(null);

		// when
		IdempotencyPort.Reservation reservation = adapter.begin(KEY);

		// then — 만료된 키 때문에 정상 요청이 막히면 안 된다
		assertThat(reservation.acquired()).isTrue();
	}

	@Test
	@DisplayName("완료 기록은 주문 ID 로 덮어쓰고 더 긴 TTL 을 건다")
	void completeStoresOrderId() {
		// given
		UUID orderId = UUID.randomUUID();

		// when
		adapter.complete(KEY, orderId);

		// then
		then(valueOperations).should()
				.set(REDIS_KEY, orderId.toString(), Duration.ofHours(24));
	}

	@Test
	@DisplayName("실패한 요청의 키는 지워 재시도를 허용한다")
	void releaseDeletesKey() {
		// when
		adapter.release(KEY);

		// then
		then(redisTemplate).should().delete(REDIS_KEY);
	}
}
