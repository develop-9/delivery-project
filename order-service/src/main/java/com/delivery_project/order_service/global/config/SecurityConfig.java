package com.delivery_project.order_service.global.config;

import com.delivery_project.order_service.global.security.JwtAuthenticationFilter;
import com.delivery_project.order_service.global.security.SecurityErrorResponder;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 게이트웨이가 JWT 를 검증한 뒤 원본 토큰을 relay 하고, 이 서비스는 토큰을 파싱해
 * 사용자 ID·역할을 얻는다.
 *
 * <p>{@code /internal/v1/**} 는 인증에서 뺀다. 서비스 간 호출에는 사용자 토큰이 없다.
 * 대신 배포 시 서비스 포트를 외부에 노출하지 않아 네트워크 레벨에서 막는다
 * (PR 리뷰 P3 에서 합의한 방식이며, 서비스 간 인증은 별도 안건이다).
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final SecurityErrorResponder securityErrorResponder;

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
								// 서비스 간 호출. 사용자 토큰이 없어 네트워크 격리로 막는다
								"/internal/v1/**",

								// actuator
								"/actuator/**",

								// swagger
								"/swagger-ui/**",
								"/v3/api-docs/**"
						).permitAll()
						// 나머지는 인증 필요
						.anyRequest().authenticated()
				)

				// 토큰 파싱 필터. 인증 여부 판단보다 앞서야 SecurityContext 가 채워진다
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

				// 인증 실패(401)와 권한 부족(403)을 나눈다. 기본 설정은 토큰이 없어도 403 을 준다
				.exceptionHandling(handler -> handler
						.authenticationEntryPoint(securityErrorResponder)
						.accessDeniedHandler(securityErrorResponder))

				// 기본 CORS 설정
				.cors(Customizer.withDefaults());

		return http.build();
	}

}
