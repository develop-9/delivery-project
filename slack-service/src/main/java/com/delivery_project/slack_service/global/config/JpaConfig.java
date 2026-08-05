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

    private static final UUID TEMP_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> Optional.of(TEMP_USER_ID);
    }
}