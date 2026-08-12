package com.delivery_project.hub_service.global.config;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import lombok.extern.slf4j.Slf4j;

/**
 * Redis 캐시 기반 설정 (00_common.md).
 *
 * <p>Cache-Aside 다 — 조회는 캐시를 먼저 보고, 변경은 <b>DB 커밋 뒤에</b> 무효화한다.
 * 커밋 전에 지우면 롤백된 트랜잭션이 멀쩡한 캐시를 날린다.
 *
 * <p><b>어떤 캐시가 있는지는 여기서 모른다.</b> 이름·TTL·값 타입은 도메인이 {@link CacheRegistrar}
 * 로 등록한다. global 은 전 서비스가 같은 코드를 쓰는 공용 영역이라 특정 도메인의 DTO 를 참조하는
 * 순간 다른 서비스로 가져갈 수 없게 된다.
 *
 * <p><b>캐시마다 값 타입을 못 박은 직렬화기를 쓴다.</b> 타입을 안 박으면 다형 타이핑을 켜야 하고,
 * 그러면 JSON 에 클래스명이 실려 역직렬화가 임의 타입 생성으로 이어질 수 있다.
 * 캐시 하나가 담는 타입은 어차피 하나뿐이라 못 박는 편이 안전하고 저장되는 JSON 도 깔끔하다.
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

	/** 등록되지 않은 캐시에 쓰이는 기본 TTL. 낡은 값이 영구히 남지 않게 하는 상한이다. */
	private static final Duration DEFAULT_TTL = Duration.ofHours(1);

	@Bean
	public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory,
			ObjectProvider<CacheRegistrar> registrars) {

		Map<String, RedisCacheConfiguration> perCache = new HashMap<>();
		registrars.orderedStream().forEach(registrar -> perCache.putAll(registrar.caches()));

		log.info("[Cache] 등록된 캐시 {}", perCache.keySet());

		return RedisCacheManager.builder(connectionFactory)
				.cacheDefaults(baseConfiguration(DEFAULT_TTL))
				.withInitialCacheConfigurations(perCache)
				// 선언한 캐시만 쓴다. 이름을 잘못 적으면 조용히 새 캐시가 생기는 대신 기동 때 드러난다.
				.disableCreateOnMissingCache()
				.build();
	}

	/**
	 * 값 타입을 못 박은 캐시 설정. {@link CacheRegistrar} 구현이 쓴다.
	 *
	 * <p>{@code static} 인 이유는 도메인 쪽 설정이 이 클래스를 주입받지 않고 부르기 때문이다 —
	 * 캐시 등록은 빈 생성 순서와 무관해야 한다.
	 */
	public static RedisCacheConfiguration typedConfiguration(Class<?> valueType, Duration ttl) {
		return baseConfiguration(ttl)
				.serializeValuesWith(RedisSerializationContext.SerializationPair
						.fromSerializer(new JacksonJsonRedisSerializer<>(valueType)));
	}

	/**
	 * Redis 장애가 API 를 죽이지 않게 한다.
	 *
	 * <p>기본 동작은 예외를 그대로 던져 500 이 나간다. 캐시는 성능 장치일 뿐이고 없어도 DB 로 답할 수 있으므로,
	 * 실패를 로그로만 남기고 원래 경로를 타게 한다. <b>무효화 실패도 삼킨다</b> —
	 * 대신 TTL 이 상한이라 낡은 값이 영구히 남지는 않는다.
	 *
	 * <p><b>걸리는 범위는 {@code CacheInterceptor} 를 타는 호출, 즉 {@code @Cacheable} 같은 애너테이션
	 * 경로뿐이다.</b> {@code CacheManager} 에서 꺼낸 {@code Cache} 를 직접 부르는 곳은 이 핸들러를
	 * 거치지 않으므로 각자 예외를 처리해야 한다 ({@code HubCacheAdapter#clear}).
	 */
	@Override
	public CacheErrorHandler errorHandler() {
		return new CacheErrorHandler() {

			@Override
			public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
				log.warn("[Cache] 조회 실패 cache={} key={} cause={}", cache.getName(), key, exception.getMessage());
			}

			@Override
			public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
				log.warn("[Cache] 저장 실패 cache={} key={} cause={}", cache.getName(), key, exception.getMessage());
			}

			@Override
			public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
				log.warn("[Cache] 무효화 실패 cache={} key={} cause={}", cache.getName(), key, exception.getMessage());
			}

			@Override
			public void handleCacheClearError(RuntimeException exception, Cache cache) {
				log.warn("[Cache] 전체 삭제 실패 cache={} cause={}", cache.getName(), exception.getMessage());
			}
		};
	}

	/**
	 * 접두사를 {@code {캐시명}::} 이 아니라 {@code {캐시명}:} 으로 바꾼다. 규약이 정한 키 형태
	 * ({@code hub:{hubId}})에 맞추기 위한 것이고, 같은 Redis 를 보는 다른 도구가 규약 그대로 키를 찾을 수 있어야 한다.
	 *
	 * <p>{@code disableCachingNullValues} 는 타입을 못 박은 직렬화기와 짝이다 —
	 * 값 타입이 고정되면 Spring 이 null 자리에 넣는 {@code NullValue} 를 직렬화할 수 없다.
	 * 조회 실패는 어차피 404 예외라 캐시할 null 도 없다.
	 */
	private static RedisCacheConfiguration baseConfiguration(Duration ttl) {
		return RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(ttl)
				.computePrefixWith(cacheName -> cacheName + ":")
				.disableCachingNullValues()
				.serializeKeysWith(RedisSerializationContext.SerializationPair
						.fromSerializer(new StringRedisSerializer()));
	}
}
