package com.delivery_project.api_gateway.global.exception;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.ConnectException;
import java.net.URI;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.resource.NoResourceFoundException;
import org.springframework.web.server.ServerWebExchange;

import com.delivery_project.api_gateway.gateway.filter.GatewayErrorResponseWriter;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * 실제 Docker 환경에서 재현해 확인한 두 예외 타입을 기준으로 분기를 검증한다 — 라우팅 대상이
 * 없는 경로는 NoResourceFoundException(404를 이미 담고 있음), 하위 서비스 연결 실패는
 * ConnectTimeoutException 계열로 서로 다른 예외였다.
 */
class GlobalExceptionHandlerTest {

	private final GatewayErrorResponseWriter errorResponseWriter = mock(GatewayErrorResponseWriter.class);
	private final GlobalExceptionHandler handler = new GlobalExceptionHandler(errorResponseWriter);

	@Test
	void 등록된_라우팅이_없으면_ROUTE_NOT_FOUND로_변환한다() {
		// given
		ServerWebExchange exchange = exchangeFor("/api/v1/products");
		NoResourceFoundException ex = new NoResourceFoundException(URI.create("/api/v1/products"), "/api/v1/products");
		when(errorResponseWriter.write(exchange, ErrorCode.ROUTE_NOT_FOUND)).thenReturn(Mono.empty());

		// when & then
		StepVerifier.create(handler.handle(exchange, ex)).verifyComplete();
		verify(errorResponseWriter).write(eq(exchange), eq(ErrorCode.ROUTE_NOT_FOUND));
	}

	@Test
	void 하위_서비스_연결_실패는_SERVICE_UNAVAILABLE로_변환한다() {
		// given
		ServerWebExchange exchange = exchangeFor("/api/v1/users/me");
		ConnectException ex = new ConnectException("connection timed out");
		when(errorResponseWriter.write(exchange, ErrorCode.SERVICE_UNAVAILABLE)).thenReturn(Mono.empty());

		// when & then
		StepVerifier.create(handler.handle(exchange, ex)).verifyComplete();
		verify(errorResponseWriter).write(eq(exchange), eq(ErrorCode.SERVICE_UNAVAILABLE));
	}

	@Test
	void 응답_타임아웃도_SERVICE_UNAVAILABLE로_변환한다() {
		// given
		ServerWebExchange exchange = exchangeFor("/api/v1/users/me");
		TimeoutException ex = new TimeoutException("Response took longer than timeout");
		when(errorResponseWriter.write(exchange, ErrorCode.SERVICE_UNAVAILABLE)).thenReturn(Mono.empty());

		// when & then
		StepVerifier.create(handler.handle(exchange, ex)).verifyComplete();
		verify(errorResponseWriter).write(eq(exchange), eq(ErrorCode.SERVICE_UNAVAILABLE));
	}

	private ServerWebExchange exchangeFor(String path) {
		return MockServerWebExchange.from(MockServerHttpRequest.get(path).build());
	}
}
