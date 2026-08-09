package com.delivery_project.hub_service.hub.application.support;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 캐시 무효화 단위 테스트. 무효화가 <b>언제</b> 일어나고 <b>실패하면 어떻게 되는지</b>를 본다. */
@ExtendWith(MockitoExtension.class)
class HubCacheEvictorTest {

	@Mock
	private CacheManager cacheManager;

	@Mock
	private Cache cache;

	@InjectMocks
	private HubCacheEvictor hubCacheEvictor;

	@AfterEach
	void clearSynchronization() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Nested
	@DisplayName("무효화 실패")
	class EvictionFailure {

		@Test
		@DisplayName("Redis 장애로 허브 캐시를 못 비워도 예외를 밖으로 내보내지 않는다")
		void swallowsHubCacheFailure() {
			// given: 무효화는 커밋 이후라 여기서 터지면 되돌릴 수도 재시도할 수도 없다
			when(cacheManager.getCache(HubCacheNames.HUB)).thenReturn(cache);
			doThrow(new QueryTimeoutException("Redis 응답 없음")).when(cache).clear();

			// when & then
			assertThatCode(() -> hubCacheEvictor.evictAllHubs()).doesNotThrowAnyException();
		}

		@Test
		@DisplayName("경로 캐시도 마찬가지다")
		void swallowsHubPathCacheFailure() {
			// given
			when(cacheManager.getCache(HubCacheNames.HUB_PATH)).thenReturn(cache);
			doThrow(new QueryTimeoutException("Redis 응답 없음")).when(cache).clear();

			// when & then
			assertThatCode(() -> hubCacheEvictor.evictAllHubPaths()).doesNotThrowAnyException();
		}

		@Test
		@DisplayName("캐시가 등록돼 있지 않으면 아무 것도 하지 않는다")
		void ignoresUnregisteredCache() {
			// given
			when(cacheManager.getCache(HubCacheNames.HUB)).thenReturn(null);

			// when & then
			assertThatCode(() -> hubCacheEvictor.evictAllHubs()).doesNotThrowAnyException();
		}
	}

	@Nested
	@DisplayName("무효화 시점")
	class EvictionTiming {

		@Test
		@DisplayName("트랜잭션 밖에서는 미룰 곳이 없으므로 즉시 비운다")
		void evictsImmediatelyOutsideTransaction() {
			// given
			when(cacheManager.getCache(HubCacheNames.HUB)).thenReturn(cache);

			// when
			hubCacheEvictor.evictAllHubs();

			// then
			verify(cache).clear();
		}

		@Test
		@DisplayName("트랜잭션 안에서는 커밋 이후로 미룬다")
		void defersUntilAfterCommit() {
			// given: 커밋 전에 지우면 그 사이 조회가 옛 값을 다시 캐시에 올린다
			when(cacheManager.getCache(HubCacheNames.HUB)).thenReturn(cache);
			TransactionSynchronizationManager.initSynchronization();

			// when
			hubCacheEvictor.evictAllHubs();

			// then: 아직 커밋 전이라 건드리지 않는다
			verify(cache, never()).clear();

			// when: 커밋됐다
			TransactionSynchronizationManager.getSynchronizations()
					.forEach(TransactionSynchronization::afterCommit);

			// then
			verify(cache).clear();
		}
	}
}
