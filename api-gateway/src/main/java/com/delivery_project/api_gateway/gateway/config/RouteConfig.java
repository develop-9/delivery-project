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
	}
}
