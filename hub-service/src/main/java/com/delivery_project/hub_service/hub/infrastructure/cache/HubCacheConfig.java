package com.delivery_project.hub_service.hub.infrastructure.cache;

import java.time.Duration;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import com.delivery_project.hub_service.global.config.CacheConfig;
import com.delivery_project.hub_service.global.config.CacheRegistrar;
import com.delivery_project.hub_service.hub.application.result.HubDetailResult;
import com.delivery_project.hub_service.hub.application.result.HubPathResult;
import com.delivery_project.hub_service.hub.application.support.HubCacheNames;

/**
 * 허브 캐시 등록 (00_common.md).
 *
 * <table>
 *   <caption>캐시 목록</caption>
 *   <tr><th>키</th><th>대상</th><th>TTL</th></tr>
 *   <tr><td>{@code hub:{hubId}}</td><td>허브 단건</td><td>24h</td></tr>
 *   <tr><td>{@code hubPath:{depId}:{arrId}}</td><td>산출된 경로</td><td>1h</td></tr>
 * </table>
 *
 * <p>문서의 {@code hub:all} · {@code hubRoute:{depId}:{arrId}} 는 만들지 않았다.
 * 전체 허브를 페이징 없이 돌려주는 API 가 없고, 구간 단건 조회는 경로 산출 안에서만 쓰여
 * 결과가 {@code hubPath} 캐시에 이미 담기기 때문이다. 소비자가 생기면 그때 추가한다.
 *
 * <p>무효화 시점은 {@link com.delivery_project.hub_service.hub.application.support.HubCacheEvictor}
 * 가 맞춘다.
 */
@Configuration
public class HubCacheConfig {

	@Bean
	public CacheRegistrar hubCacheRegistrar() {
		return () -> Map.of(
				HubCacheNames.HUB,
				CacheConfig.typedConfiguration(HubDetailResult.class, Duration.ofHours(24)),

				HubCacheNames.HUB_PATH,
				CacheConfig.typedConfiguration(HubPathResult.class, Duration.ofHours(1))
		);
	}
}
