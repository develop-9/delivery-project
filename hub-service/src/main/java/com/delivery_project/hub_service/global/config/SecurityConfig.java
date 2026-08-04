package com.delivery_project.hub_service.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * TODO 현재는 모든 요청을 통과시킨다. JWT 파싱 필터와 권한 검증은 인증 단계에서 추가한다.
 *
 * <p>게이트웨이가 JWT 를 검증한 뒤 원본 토큰을 relay 하며, 이 서비스는 토큰을 파싱해
 * 사용자 ID·권한·소속 허브를 얻는다. 클레임명은 user-service 담당자와 확정 후 반영한다.
 *
 * <p>이 설정이 없으면 Spring Security 기본 정책이 전 요청에 인증을 요구해
 * compose healthcheck 가 쓰는 {@code /actuator/health} 가 401 을 반환한다.
 */
@Configuration
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.httpBasic(httpBasic -> httpBasic.disable())
				.formLogin(formLogin -> formLogin.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
				.build();
	}
}
