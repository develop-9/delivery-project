package com.delivery_project.api_gateway.gateway.filter;

import java.util.Set;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.delivery_project.api_gateway.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Gateway는 서명 검증 + 만료 확인 + 무효화 여부(블랙리스트)만 확인하는 순수 인증만 담당한다.
 * role/리소스 인가는 각 서비스가 JWT claim으로 자체 처리하므로, 검증을 통과한 요청은 원본 JWT를
 * Authorization 헤더에 그대로 실은 채(수정 없이) 하위 서비스로 relay된다.
 *
 * 토큰 검증(jjwt)과 무효화 여부 조회(Redis)는 각각 TokenValidator/TokenBlacklistChecker
 * 구현체가 담당하고, 이 필터는 그 결과를 조합해 통과/차단만 결정하는 오케스트레이터로 유지한다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

	private static final String BEARER_PREFIX = "Bearer ";

	private static final Set<String> WHITELIST_PATHS = Set.of(
			"/api/v1/auth/signup",
			"/api/v1/auth/login",
			"/api/v1/auth/refresh"
	);

	private final TokenValidator tokenValidator;
	private final TokenBlacklistChecker tokenBlacklistChecker;
	private final GatewayErrorResponseWriter errorResponseWriter;

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
			return errorResponseWriter.write(exchange, ErrorCode.AUTH_TOKEN_INVALID);
		}

		ValidatedToken validatedToken;
		try {
			validatedToken = tokenValidator.validate(token);
		} catch (ExpiredTokenException e) {
			return errorResponseWriter.write(exchange, ErrorCode.AUTH_TOKEN_EXPIRED);
		} catch (InvalidTokenException e) {
			return errorResponseWriter.write(exchange, ErrorCode.AUTH_TOKEN_INVALID);
		}

		return tokenBlacklistChecker.isRevoked(validatedToken.userId(), validatedToken.issuedAtMillis())
				.flatMap(revoked -> revoked
						? errorResponseWriter.write(exchange, ErrorCode.AUTH_TOKEN_REVOKED)
						: chain.filter(exchange));
	}

	private String resolveToken(HttpHeaders headers) {
		String authorizationHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
		if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
			return null;
		}
		return authorizationHeader.substring(BEARER_PREFIX.length());
	}
}
