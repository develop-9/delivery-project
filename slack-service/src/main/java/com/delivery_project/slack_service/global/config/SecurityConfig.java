package com.delivery_project.slack_service.global.config;

import com.delivery_project.slack_service.global.security.JwtProvider;
import lombok.RequiredArgsConstructor;
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

/**
 * 게이트웨이가 JWT를 검증한 뒤 원본 토큰을 relay하고,
 * 이 서비스는 토큰을 파싱해 사용자 ID·권한을 얻는다.
 *
 * MASTER 전용 판정은 여기가 아니라 서비스 메서드의
 * @PreAuthorize가 한다 (@EnableMethodSecurity).
 *
 * /internal/v1/**는 인증을 걸지 않는다.
 * 접근 통제는 게이트웨이 라우팅이 외부 인입을 막는 데 의존한다.
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
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers(
                                        "/internal/v1/**",
                                        "/actuator/**",
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/v3/api-docs/**"
                                )
                                .permitAll()
                                .anyRequest()
                                .authenticated()
                )

                .exceptionHandling(handling ->
                        handling
                                .authenticationEntryPoint(
                                        jsonAuthenticationEntryPoint
                                )
                                .accessDeniedHandler(
                                        jsonAccessDeniedHandler
                                )
                )

                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtProvider),
                        UsernamePasswordAuthenticationFilter.class
                )

                .cors(Customizer.withDefaults());

        return http.build();
    }
}