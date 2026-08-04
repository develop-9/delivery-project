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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

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
								// ===개발을 위한 권한 허용===
								"/api/v1/**",
								"/internal/v1/**",
								// ======================
								"/actuator/**",
								"/swagger-ui/**",
								"/v3/api-docs/**"
						).permitAll()
						// TODO(로그인 이슈): JwtAuthenticationFilter 등록 + 위 개발용 permitAll 구간을
						// 실제 엔드포인트별 인가 규칙(로그인/회원가입만 permitAll, 나머지는 인증)으로 교체
						.anyRequest().authenticated()
				)
				.cors(Customizer.withDefaults());

		return http.build();
	}
}
