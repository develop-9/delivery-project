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
 * TODO 현재는 개발을 위해 API 경로를 열어둔다. JWT 파싱 필터와 권한 검증은 인증 단계에서 추가한다.
 *
 * <p>게이트웨이가 JWT 를 검증한 뒤 원본 토큰을 relay 하며, 이 서비스는 토큰을 파싱해
 * 사용자 ID·권한을 얻는다. 클레임명은 user-service 담당자와 확정 후 반영한다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(
			HttpSecurity http
	) throws Exception {

		http
				// CSRF 비활성화
				.csrf(AbstractHttpConfigurer::disable)
				// Form Login 비활성화
				.formLogin(AbstractHttpConfigurer::disable)
				// HTTP Basic 비활성화
				.httpBasic(AbstractHttpConfigurer::disable)
				// Session 사용 안 함 (JWT 기반)
				.sessionManagement(session ->
						session.sessionCreationPolicy(
								SessionCreationPolicy.STATELESS
						)
				)
				// Authorization
				.authorizeHttpRequests(auth -> auth
						// 인증 없이 접근 가능
						.requestMatchers(
								// ===개발을 위한 권한 허용===
								"/api/v1/**",
								"/internal/v1/**",
								// ======================

								// actuator
								"/actuator/**",

								// swagger
								"/swagger-ui/**",
								"/v3/api-docs/**"
						).permitAll()
						// 나머지는 인증 필요
						.anyRequest().authenticated()
				)

				// 기본 CORS 설정
				.cors(Customizer.withDefaults());

		return http.build();
	}

}
