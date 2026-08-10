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
 * <p>{@code /internal/v1/**} 는 등록하지 않는다. 서비스 간 직접 호출용이라 게이트웨이를 거치지
 * 않으며, 외부에 노출되면 인증 없이 내부 API 를 부를 수 있게 된다.
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
