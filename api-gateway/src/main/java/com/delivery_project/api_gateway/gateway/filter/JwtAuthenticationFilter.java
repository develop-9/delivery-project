package com.delivery_project.api_gateway.gateway.filter;

import java.util.Set;

import javax.crypto.SecretKey;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.delivery_project.api_gateway.global.exception.ErrorCode;
import com.delivery_project.api_gateway.global.response.ErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Gateway는 서명 검증 + 만료 확인 + 무효화 여부(블랙리스트)만 확인하는 순수 인증만 담당한다.
 * role/리소스 인가는 각 서비스가 JWT claim으로 자체 처리하므로, 검증을 통과한 요청은 원본 JWT를
 * Authorization 헤더에 그대로 실은 채(수정 없이) 하위 서비스로 relay된다.
 *
 * Redis 장애 시 무효화 여부를 확인할 수 없는 경우 fail-open(차단하지 않고 통과)으로 처리한다.
 * fail-open/fail-closed 정책은 팀 논의가 필요한 사안이라 우선 가용성을 우선하는 쪽으로 잡아뒀다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

	private static final String BEARER_PREFIX = "Bearer ";
	private static final String INVALIDATED_AT_KEY_PREFIX = "user:";
	private static final String INVALIDATED_AT_KEY_SUFFIX = ":invalidatedAt";

	private static final Set<String> WHITELIST_PATHS = Set.of(
			"/api/v1/auth/signup",
			"/api/v1/auth/login",
			"/api/v1/auth/refresh"
	);

	private final SecretKey jwtSecretKey;
	private final ReactiveStringRedisTemplate reactiveStringRedisTemplate;
	private final ObjectMapper objectMapper;

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 10;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		String path = exchange.getRequest().getURI().getPath();
		if (WHITELIST_PATHS.contains(path)) {
			return chain.filter(exchange);
		}

		String token = resolveToken(exchange.getRequest().getHeaders());
		if (token == null) {
			return reject(exchange, ErrorCode.AUTH_TOKEN_INVALID);
		}

		Claims claims;
		try {
			claims = Jwts.parser().verifyWith(jwtSecretKey).build().parseSignedClaims(token).getPayload();
		} catch (ExpiredJwtException e) {
			return reject(exchange, ErrorCode.AUTH_TOKEN_EXPIRED);
		} catch (JwtException | IllegalArgumentException e) {
			return reject(exchange, ErrorCode.AUTH_TOKEN_INVALID);
		}

		String userId = claims.getSubject();
		long issuedAtMillis = claims.getIssuedAt().getTime();

		return reactiveStringRedisTemplate.opsForValue().get(invalidatedAtKey(userId))
				.map(Long::parseLong)
				.defaultIfEmpty(Long.MIN_VALUE)
				.onErrorResume(e -> {
					log.warn("[Gateway] 무효화 여부 확인 중 Redis 조회 실패, fail-open으로 통과 userId={}", userId, e);
					return Mono.just(Long.MIN_VALUE);
				})
				.flatMap(invalidatedAtMillis -> {
					if (issuedAtMillis < invalidatedAtMillis) {
						return reject(exchange, ErrorCode.AUTH_TOKEN_REVOKED);
					}
					return chain.filter(exchange);
				});
	}

	private String resolveToken(HttpHeaders headers) {
		String authorizationHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			return null;
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}

	private String invalidatedAtKey(String userId) {
		return INVALIDATED_AT_KEY_PREFIX + userId + INVALIDATED_AT_KEY_SUFFIX;
	}

	private Mono<Void> reject(ServerWebExchange exchange, ErrorCode errorCode) {
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(errorCode.getHttpStatus());
		response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

		byte[] body;
		try {
			body = objectMapper.writeValueAsBytes(ErrorResponse.from(errorCode));
		} catch (JsonProcessingException e) {
			body = new byte[0];
		}

		DataBuffer buffer = response.bufferFactory().wrap(body);
		return response.writeWith(Mono.just(buffer));
	}
}
