package com.delivery_project.delivery_service.global.config;

import com.delivery_project.delivery_service.global.security.JwtPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

@Configuration
@EnableJpaAuditing
public class JpaConfig {

    @Value("${system.id}")
    private UUID systemId;

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> Optional.ofNullable(
                        SecurityContextHolder.getContext()
                                .getAuthentication()
                )
                .filter(authentication -> authentication.isAuthenticated())
                .map(authentication -> authentication.getPrincipal())
                .filter(JwtPrincipal.class::isInstance)
                .map(JwtPrincipal.class::cast)
                .map(JwtPrincipal::userId)
                .or(() -> Optional.of(systemId));
    }
}