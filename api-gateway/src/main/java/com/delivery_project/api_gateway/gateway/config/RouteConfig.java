package com.delivery_project.api_gateway.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * User Service 경로만 우선 라우팅한다. 다른 서비스는 각 담당자가 자기 경로를 추가하기 전까지
 * Gateway를 거치지 않는다.
 */
@Configuration
public class RouteConfig {

	@Bean
	public RouteLocator userServiceRoutes(RouteLocatorBuilder builder) {
		return builder.routes()
				.route("user-service", r -> r
						.path("/api/v1/auth/**", "/api/v1/users/**")
						.uri("lb://USER-SERVICE"))
				.build();
	}

	@Bean
	public RouteLocator deliveryServiceRoutes(
			RouteLocatorBuilder builder
	) {
		return builder.routes()
				.route("delivery-service", r -> r
						.path(
								"/api/v1/delivery-managers/**",
								"/api/v1/deliveries/**",
								"/api/v1/delivery-routes/**"
						)
						.uri("lb://DELIVERY-SERVICE"))
				.build();
	/**
	 * {@code /internal/v1/hubs/**} · {@code /internal/v1/hub-routes/**} 는 여기 넣지 않는다.
	 * 내부 API 는 Feign 직접 호출 전용이고, hub-service 가 그 경로를 permitAll 로 여는 근거가
	 * "Gateway 라우팅에 없어서 외부에서 닿을 수 없다" 는 것이라 등록하는 순간 무인증으로 열린다.
	 */
	@Bean
	public RouteLocator hubServiceRoutes(RouteLocatorBuilder builder) {
		return builder.routes()
				.route("hub-service", r -> r
						.path("/api/v1/hubs/**", "/api/v1/hub-routes/**")
						.uri("lb://HUB-SERVICE"))
				.build();
	}
}
