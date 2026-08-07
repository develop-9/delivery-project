package com.delivery_project.hub_service.hub.application.support;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 허브 캐시 무효화를 <b>DB 커밋 이후로</b> 미룬다 (Cache-Aside).
 *
 * <p>{@code @CacheEvict} 를 쓰지 않는 이유가 여기 있다. 그 애너테이션은 메서드가 정상 반환하면
 * 지우는데, {@code @Transactional} 메서드에서는 <b>아직 커밋 전</b>이다. 그 사이에 다른 요청이
 * 조회하면 옛 값이 다시 캐시에 올라가고, 트랜잭션이 롤백되면 멀쩡한 캐시만 날린 꼴이 된다.
 *
 * <p>트랜잭션 밖에서 불리면 미룰 곳이 없으므로 즉시 지운다.
 *
 * <p>무효화 대상이 허브 캐시로 한정되므로 {@code global} 이 아니라 이 도메인이 들고 있는다.
 * 어떤 직렬화기로 얼마나 담아둘지는 {@code hub/infrastructure/cache} 가 정한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HubCacheEvictor {

	private final CacheManager cacheManager;

	/**
	 * 허브 단건 캐시 전체. 허브 수정·삭제 시 무효화한다.
	 *
	 * <p>바뀐 허브의 키 하나만 지워서는 부족하다. {@code HubDetailResult} 는 상위 허브에서 복사해 온
	 * {@code parentHubName} 을 함께 담기 때문에, 중앙 허브의 이름이 바뀌면 그 값을 품고 있는
	 * <b>하위 허브들의 캐시</b>까지 낡는다. 어떤 키가 영향받는지는 캐시 키만 봐서는 알 수 없다.
	 *
	 * <p>그래서 통째로 비운다. <b>허브는 17개로 고정이고 수정이 사실상 없어</b> 다시 채우는 비용이
	 * 무시할 만하다. 경로 캐시({@link #evictAllHubPaths})가 같은 이유로 같은 선택을 했고,
	 * 무효화 규칙을 하나로 두면 응답에 담기는 필드가 늘어도 빠뜨릴 곳이 생기지 않는다.
	 */
	public void evictAllHubs() {
		afterCommit(() -> clear(HubCacheNames.HUB));
	}

	/**
	 * 산출된 경로 캐시 전체.
	 *
	 * <p>키 하나만 지울 수 없다 — 허브나 구간 하나가 바뀌면 그것을 지나는 모든 (출발, 도착) 쌍의
	 * 경로가 함께 틀어지고, 어떤 쌍이 영향받는지는 캐시 키만 봐서 알 수 없기 때문이다.
	 * 경로 캐시는 TTL 이 1시간이라 통째로 비워도 다시 채우는 비용이 크지 않다.
	 */
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

	private void clear(String cacheName) {
		Cache cache = cacheManager.getCache(cacheName);

		if (cache != null) {
			cache.clear();
			log.debug("[Cache] 전체 무효화 cache={}", cacheName);
		}
	}
}
