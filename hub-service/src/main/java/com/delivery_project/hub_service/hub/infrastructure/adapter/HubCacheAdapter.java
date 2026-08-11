package com.delivery_project.hub_service.hub.infrastructure.adapter;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.delivery_project.hub_service.hub.application.port.HubCachePort;
import com.delivery_project.hub_service.hub.application.support.HubCacheNames;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * {@link HubCachePort} 를 Spring Cache 로 구현한다 (Cache-Aside).
 *
 * <p>{@code @CacheEvict} 를 쓰지 않는 이유가 여기 있다. 그 애너테이션은 메서드가 정상 반환하면
 * 지우는데, {@code @Transactional} 메서드에서는 <b>아직 커밋 전</b>이다. 그 사이에 다른 요청이
 * 조회하면 옛 값이 다시 캐시에 올라가고, 트랜잭션이 롤백되면 멀쩡한 캐시만 날린 꼴이 된다.
 *
 * <p>트랜잭션 밖에서 불리면 미룰 곳이 없으므로 즉시 지운다.
 *
 * <p>어떤 직렬화기로 얼마나 담아둘지는 {@code hub/infrastructure/config/HubCacheConfig} 가 정한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HubCacheAdapter implements HubCachePort {

	private final CacheManager cacheManager;

	@Override
	public void evictAllHubs() {
		afterCommit(() -> clear(HubCacheNames.HUB));
	}

	@Override
	public void evictAllHubPaths() {
		afterCommit(() -> clear(HubCacheNames.HUB_PATH));
	}

	private void afterCommit(Runnable eviction) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			eviction.run();
			return;
		}

		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				eviction.run();
			}
		});
	}

	/**
	 * Redis 장애가 이 지점에서 API 를 실패시키지 않게 한다.
	 *
	 * <p>{@code CacheConfig} 의 {@code CacheErrorHandler} 는 {@code @Cacheable} 처럼
	 * {@code CacheInterceptor} 를 타는 호출에만 걸린다. 여기는 {@code Cache} API 를 직접 부르므로
	 * 그 핸들러를 거치지 않아, 감싸지 않으면 예외가 그대로 올라간다.
	 *
	 * <p>하필 {@link #afterCommit} 안이라 더 나쁘다 — DB 는 이미 커밋됐는데 클라이언트만 실패 응답을
	 * 받고, 되돌릴 수도 재시도할 수도 없다. 캐시는 성능 장치일 뿐이므로 로그만 남기고 넘어간다.
	 * 낡은 값은 TTL(허브 24h · 경로 1h)이 상한이라 영구히 남지 않는다.
	 */
	private void clear(String cacheName) {
		Cache cache = cacheManager.getCache(cacheName);

		if (cache == null) {
			return;
		}

		try {
			cache.clear();
			log.debug("[Cache] 전체 무효화 cache={}", cacheName);
		} catch (RuntimeException e) {
			log.warn("[Cache] 전체 무효화 실패 cache={}", cacheName, e);
		}
	}
}
