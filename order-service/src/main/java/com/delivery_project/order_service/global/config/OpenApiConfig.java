package com.delivery_project.order_service.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Order Service API")
                .version("v1")
                .description("""
                        주문 · 재고 · 주문 이력(스냅샷) API

                        인증: api-gateway 가 JWT 를 검증한 뒤 X-User-Id / X-User-Role 헤더를 주입한다.
                        Gateway 없이 직접 호출하면 order.auth.dev-user-* 설정의 개발용 사용자로 동작한다.
                        """));
    }
}
