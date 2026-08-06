package com.delivery_project.slack_service.global.config;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

    // @CreatedBy/@LastModifiedBy를 채우는 데 사용한다.
    // 인증 정보가 없거나 principal이 UUID가 아니면 systemId를 사용한다.
    @Bean
    public AuditorAware<UUID> auditorProvider(
            @Value("${system.id}") UUID systemId
    ) {
        return () -> Optional.of(
                Optional.ofNullable(
                                SecurityContextHolder
                                        .getContext()
                                        .getAuthentication()
                        )
                        .filter(Authentication::isAuthenticated)
                        .map(Authentication::getPrincipal)
                        .filter(UUID.class::isInstance)
                        .map(UUID.class::cast)
                        .orElse(systemId)
        );
    }
}