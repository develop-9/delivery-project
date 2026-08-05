package com.delivery_project.order_service.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 인증·인가는 api-gateway 가 JWT 를 검증해 처리한다.
 * order-service 는 Gateway 가 주입한 X-User-Id / X-User-Role 헤더를
 * UserContextInterceptor 로 읽어 쓰기만 하므로, 필터 체인은 전부 열어 둔다.
 *
 * ⚠️ 이 설정만 두면 서비스 포트(9004)로 직접 들어오는 요청도 통과한다.
 *    운영에서는 Gateway 외 접근을 네트워크 레벨에서 막고,
 *    order.auth.require-gateway-headers=true 로 인증 헤더를 강제해야 한다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // CSRF 비활성화 (세션을 쓰지 않는 REST API)
                .csrf(AbstractHttpConfigurer::disable)
                // Form Login 비활성화
                .formLogin(AbstractHttpConfigurer::disable)
                // HTTP Basic 비활성화 — 켜져 있으면 브라우저 인증 팝업이 뜬다
                .httpBasic(AbstractHttpConfigurer::disable)
                // Session 사용 안 함 (JWT 기반)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 모든 요청 인증 없이 통과
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                // 기본 CORS 설정
                .cors(Customizer.withDefaults());

        return http.build();
    }
}
