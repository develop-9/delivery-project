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

                        인증: api-gateway 가 JWT 를 검증한 뒤 원본 토큰을 relay 하고,
                        각 서비스가 Authorization 헤더의 토큰을 파싱해 인증 주체를 얻는다
                        (user-service 의 JwtAuthenticationFilter 와 동일한 방식).
                        JWT 파싱 필터가 붙기 전까지 인증 주체가 없는 요청은 system.id 로 기록된다.
                        """));
    }
}
