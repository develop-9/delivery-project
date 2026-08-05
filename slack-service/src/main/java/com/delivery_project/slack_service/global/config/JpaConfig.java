package com.delivery_project.slack_service.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;
import java.util.UUID;

@Configuration
@EnableJpaAuditing
public class JpaConfig {

    // TODO: JWT 인증 적용 후 현재 로그인 사용자의 UUID를 반환하도록 변경
    private static final UUID TEMP_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> Optional.of(TEMP_USER_ID);
    }
}