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

	@Bean
	public RouteLocator slackServiceRoutes(RouteLocatorBuilder builder) {
		return builder.routes()
				.route("slack-service", r -> r
						.path("/api/v1/slack-messages/**", "/api/v1/ai-histories/**")
						.uri("lb://SLACK-SERVICE"))
				.build();
	}

	/**
	 * 서비스별 Swagger 문서(/v3/api-docs)를 Gateway가 한곳(/swagger-ui.html)에서 모아 보여주기
	 * 위한 프록시 라우트. {@code application.yaml}의 springdoc.swagger-ui.urls가 여기 등록된
	 * /docs/{서비스명}/v3/api-docs 경로를 그대로 가리킨다 — 실제 서비스로 직접 요청하면 CORS
	 * 설정이 없어 브라우저에서 막히므로, Gateway를 거쳐 같은 origin으로 보이게 한다.
	 *
	 * Delivery Service/Order Service는 아직 springdoc 의존성이 없어 /v3/api-docs 자체가
	 * 없다 — 여기 추가하지 않았다. 두 서비스에 springdoc이 추가되면 라우트만 더하면 된다.
	 */
	@Bean
	public RouteLocator apiDocsRoutes(RouteLocatorBuilder builder) {
		return builder.routes()
				.route("user-service-docs", r -> r
						.path("/docs/user-service/v3/api-docs")
						.filters(f -> f.rewritePath("/docs/user-service/v3/api-docs", "/v3/api-docs"))
						.uri("lb://USER-SERVICE"))
				.route("company-service-docs", r -> r
						.path("/docs/company-service/v3/api-docs")
						.filters(f -> f.rewritePath("/docs/company-service/v3/api-docs", "/v3/api-docs"))
						.uri("lb://COMPANY-SERVICE"))
				.route("hub-service-docs", r -> r
						.path("/docs/hub-service/v3/api-docs")
						.filters(f -> f.rewritePath("/docs/hub-service/v3/api-docs", "/v3/api-docs"))
						.uri("lb://HUB-SERVICE"))
				.route("slack-service-docs", r -> r
						.path("/docs/slack-service/v3/api-docs")
						.filters(f -> f.rewritePath("/docs/slack-service/v3/api-docs", "/v3/api-docs"))
						.uri("lb://SLACK-SERVICE"))
				.build();
	}
}
