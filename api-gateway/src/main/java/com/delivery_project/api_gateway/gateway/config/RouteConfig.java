package com.delivery_project.api_gateway.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 서비스별 라우팅. 각 담당자가 자기 서비스 경로를 {@code RouteLocator} 빈으로 추가한다.
 *
 * <p>담당자마다 메서드를 따로 두는 것은 같은 파일을 여럿이 동시에 고칠 때 충돌을 줄이기 위해서다.
 *
 * <p>{@code /internal/v1/**} 는 등록하지 않는다. 내부 API 는 Feign 직접 호출 전용이고,
 * 각 서비스가 그 경로를 permitAll 로 여는 근거가 "Gateway 라우팅에 없어서 외부에서 닿을 수 없다"
 * 는 것이라 등록하는 순간 무인증으로 열린다.
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
	public RouteLocator companyServiceRoutes(RouteLocatorBuilder builder) {
		return builder.routes()
				.route("company-service", r -> r
						.path("/api/v1/companies/**", "/api/v1/products/**")
						.uri("lb://COMPANY-SERVICE"))
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

	/**
	 * 주문 · 주문 이력 · 재고.
	 *
	 * <p>{@code order-snapshots} 는 {@code orders} 하위가 아니라 독립 경로라 따로 적는다
	 * (주문 하위 타임라인은 {@code /api/v1/orders/**} 에 걸린다).
	 */
	@Bean
	public RouteLocator orderServiceRoutes(RouteLocatorBuilder builder) {
		return builder.routes()
				.route("order-service", r -> r
						.path("/api/v1/orders/**", "/api/v1/order-snapshots/**", "/api/v1/inventories/**")
						.uri("lb://ORDER-SERVICE"))
				.build();
	}
}
