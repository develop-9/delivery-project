package com.delivery_project.hub_service.global.util;

import java.util.UUID;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.delivery_project.hub_service.global.config.CacheConfig;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 캐시 무효화를 <b>DB 커밋 이후로</b> 미룬다 (Cache-Aside).
 *
 * <p>{@code @CacheEvict} 를 쓰지 않는 이유가 여기 있다. 그 애너테이션은 메서드가 정상 반환하면
 * 지우는데, {@code @Transactional} 메서드에서는 <b>아직 커밋 전</b>이다. 그 사이에 다른 요청이
 * 조회하면 옛 값이 다시 캐시에 올라가고, 트랜잭션이 롤백되면 멀쩡한 캐시만 날린 꼴이 된다.
 *
 * <p>트랜잭션 밖에서 불리면 미룰 곳이 없으므로 즉시 지운다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheEvictor {

	private final CacheManager cacheManager;

	/** 허브 단건 캐시. 해당 허브의 생성·수정·삭제 시 무효화한다. */
	public void evictHub(UUID hubId) {
		afterCommit(() -> evictKey(CacheConfig.HUB_CACHE, hubId.toString()));
	}

	/**
	 * 산출된 경로 캐시 전체.
	 *
	 * <p>키 하나만 지울 수 없다 — 허브나 구간 하나가 바뀌면 그것을 지나는 모든 (출발, 도착) 쌍의
	 * 경로가 함께 틀어지고, 어떤 쌍이 영향받는지는 캐시 키만 봐서 알 수 없기 때문이다.
	 * 최대 17×16 쌍이고 TTL 도 1시간이라 통째로 비우는 비용이 크지 않다.
	 */
	public void evictAllHubPaths() {
		afterCommit(() -> clear(CacheConfig.HUB_PATH_CACHE));
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

	private void evictKey(String cacheName, String key) {
		Cache cache = cacheManager.getCache(cacheName);

		if (cache != null) {
			cache.evict(key);
			log.debug("[Cache] 무효화 cache={} key={}", cacheName, key);
		}
	}

	private void clear(String cacheName) {
		Cache cache = cacheManager.getCache(cacheName);

		if (cache != null) {
			cache.clear();
			log.debug("[Cache] 전체 무효화 cache={}", cacheName);
		}
	}
}
