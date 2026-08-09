package com.delivery_project.api_gateway.gateway.filter;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.delivery_project.api_gateway.global.exception.ErrorCode;
import com.delivery_project.api_gateway.global.response.ErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class GatewayErrorResponseWriter {

	private final ObjectMapper objectMapper;

	public Mono<Void> write(ServerWebExchange exchange, ErrorCode errorCode) {
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
