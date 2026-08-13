package com.delivery_project.api_gateway.global.exception;

import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import com.delivery_project.api_gateway.gateway.filter.GatewayErrorResponseWriter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * JwtAuthenticationFilter가 처리하는 인증 오류(401)는 이미 자체적으로 응답을 쓰고 끝나므로
 * 여기까지 오지 않는다. 이 핸들러는 그 외 Gateway 인프라 레벨 예외(라우팅 대상 서비스를
 * 못 찾음, 하위 서비스 연결 실패/타임아웃 등)를 팀 공통 응답 포맷으로 변환한다.
 * DefaultErrorWebExceptionHandler(Order -1)보다 먼저 실행되도록 -2로 순서를 앞당긴다.
 */
@Slf4j
@Component
@Order(-2)
@RequiredArgsConstructor
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

	private final GatewayErrorResponseWriter errorResponseWriter;

	@Override
	public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
		log.error("[Gateway] 처리되지 않은 예외 발생 path={}", exchange.getRequest().getPath(), ex);
		return errorResponseWriter.write(exchange, resolveErrorCode(ex));
	}

	/**
	 * 라우팅 대상을 못 찾으면(등록된 경로 자체가 없음) WebFlux가 정적 리소스 핸들러까지 떨어져서
	 * NoResourceFoundException(404를 이미 담고 있는 ResponseStatusException)을 던진다. 이걸
	 * 구분 안 하고 무조건 503으로 덮어쓰면, 클라이언트가 존재하지 않는 경로를 요청한 것과 하위
	 * 서비스가 실제로 죽은 것을 구분할 수 없다(직접 재현해서 확인: 라우팅 실패는
	 * NoResourceFoundException, 하위 서비스 연결 실패는 ConnectTimeoutException으로 서로 다른
	 * 예외였다). 연결 실패/타임아웃 등 나머지는 지금처럼 503이 맞으므로 그대로 둔다.
	 */
	private ErrorCode resolveErrorCode(Throwable ex) {
		if (ex instanceof ResponseStatusException responseStatusException
				&& responseStatusException.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
			return ErrorCode.ROUTE_NOT_FOUND;
		}
		return ErrorCode.SERVICE_UNAVAILABLE;
	}
}
