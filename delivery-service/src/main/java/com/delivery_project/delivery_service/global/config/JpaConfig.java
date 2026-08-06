package com.delivery_project.delivery_service.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;
import java.util.UUID;

@Configuration
@EnableJpaAuditing
public class JpaConfig { // 임시 처리

    @Value("${system.id}")
    private UUID systemId;

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> Optional.of(systemId);
    }
}