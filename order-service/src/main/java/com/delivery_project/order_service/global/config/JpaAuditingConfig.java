package com.delivery_project.order_service.global.config;

import com.delivery_project.order_service.global.security.UserContext;
import com.delivery_project.order_service.global.security.UserContextHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;
import java.util.UUID;

@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    /** 시스템(내부 호출·배치)이 만든 데이터의 작성자 */
    private static final UUID SYSTEM_USER_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Bean
    public AuditorAware<UUID> auditorProvider() {
        return () -> {
            UserContext context = UserContextHolder.get();
            return Optional.of(context != null ? context.userId() : SYSTEM_USER_ID);
        };
    }
}
