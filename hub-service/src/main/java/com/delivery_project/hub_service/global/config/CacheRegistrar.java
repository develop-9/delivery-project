package com.delivery_project.hub_service.global.config;

import java.util.Map;

import org.springframework.data.redis.cache.RedisCacheConfiguration;

/**
 * 도메인이 자기 캐시를 등록하는 자리.
 *
 * <p>{@link CacheConfig} 는 Redis 를 어떻게 붙일지만 알고 <b>어떤 캐시가 있는지는 모른다.</b>
 * 캐시 이름·TTL·값 타입은 그 값을 만드는 도메인만 알 수 있고, global 이 특정 도메인의 DTO 를
 * 참조하면 공통 모듈로 떼어낼 수 없기 때문이다.
 *
 * <p>구현은 {@code @Configuration} 에서 빈으로 내놓으면 된다. 여러 개여도 전부 합쳐진다.
 */
@FunctionalInterface
public interface CacheRegistrar {

	/** 캐시 이름 → 그 캐시의 설정. 이름이 겹치면 나중에 등록된 쪽이 이긴다. */
	Map<String, RedisCacheConfiguration> caches();
}
