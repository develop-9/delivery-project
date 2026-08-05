package com.delivery_project.hub_service.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.delivery_project.hub_service.global.security.JwtProvider;

import lombok.RequiredArgsConstructor;

/**
 * 게이트웨이가 JWT 를 검증한 뒤 원본 토큰을 relay 하고, 이 서비스는 토큰을 파싱해 사용자 ID·권한을 얻는다.
 *
 * <p>{@code MASTER} 전용 판정은 여기가 아니라 서비스 메서드의 {@code @PreAuthorize} 가 한다
 * ({@code @EnableMethodSecurity}). 경로 패턴으로 나누면 같은 {@code /api/v1/hubs} 를
 * 메서드별로 갈라야 해 규칙이 흩어지기 때문이다.
 *
 * <p><b>{@code /internal/**} 은 인증을 걸지 않는다.</b> 문서(03_internal.md)대로 내부 API 는
 * 호출 주체를 식별할 수단이 없어 403 을 낼 수 없고, 접근 통제는 전적으로 게이트웨이 라우팅이
 * 외부 인입을 막는 데 의존한다. 라우팅이 잘못 열려도 이 서비스는 탐지하지 못한다.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtProvider jwtProvider;
	private final JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint;
	private final JsonAccessDeniedHandler jsonAccessDeniedHandler;

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
								// 내부 API — 게이트웨이가 외부 인입을 차단한다
								"/internal/v1/**",

								// actuator
								"/actuator/**",

								// swagger
								"/swagger-ui/**",
								"/swagger-ui.html",
								"/v3/api-docs/**"
						).permitAll()
						// 나머지는 인증 필요
						.anyRequest().authenticated()
				)

				// 인증·인가 실패를 팀 공통 에러 형식으로 응답
				.exceptionHandling(handling -> handling
						.authenticationEntryPoint(jsonAuthenticationEntryPoint)
						.accessDeniedHandler(jsonAccessDeniedHandler)
				)

				.addFilterBefore(new JwtAuthenticationFilter(jwtProvider),
						UsernamePasswordAuthenticationFilter.class)

				// 기본 CORS 설정
				.cors(Customizer.withDefaults());

		return http.build();
	}

}
