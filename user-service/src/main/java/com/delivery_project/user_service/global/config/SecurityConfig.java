package com.delivery_project.user_service.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.delivery_project.user_service.user.infrastructure.jwt.JwtProvider;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtProvider jwtProvider;
	private final JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint;
	private final JsonAccessDeniedHandler jsonAccessDeniedHandler;

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/api/v1/auth/signup",
								"/api/v1/auth/login",
								"/api/v1/auth/refresh",
								// 내부 서비스 간 호출은 Gateway를 거치지 않고 Docker 내부망 안에서만 도달 가능해서
								// 별도의 서비스 신원 검증(key/mTLS) 없이 네트워크 격리에만 의존하기로 팀 확정됨
								"/internal/v1/**",
								"/actuator/**",
								"/swagger-ui/**",
								"/v3/api-docs/**"
						).permitAll()
						.anyRequest().authenticated()
				)
				.exceptionHandling(handling -> handling
						.authenticationEntryPoint(jsonAuthenticationEntryPoint)
						.accessDeniedHandler(jsonAccessDeniedHandler)
				)
				.addFilterBefore(new JwtAuthenticationFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class)
				.cors(Customizer.withDefaults());

		return http.build();
	}
}
