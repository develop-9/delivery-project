package com.delivery_project.delivery_service.global.config;

import com.delivery_project.delivery_service.global.security.JwtPrincipal;
import com.delivery_project.delivery_service.global.security.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "management.tracing.enabled=false",
        "system.id=00000000-0000-0000-0000-000000000001"
})
class JpaConfigTest {

    @Autowired
    private AuditorAware<UUID> auditorAware;

    @Value("${system.id}")
    private UUID systemId;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("JwtPrincipal이 있으면 userId를 감사 사용자로 반환한다")
    void authenticatedUser() {
        // given
        UUID userId = UUID.randomUUID();

        JwtPrincipal principal =
                new JwtPrincipal(
                        userId,
                        Role.MASTER
                );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of()
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        // when
        UUID auditor =
                auditorAware.getCurrentAuditor()
                        .orElseThrow();

        // then
        assertEquals(userId, auditor);
    }

    @Test
    @DisplayName("인증 정보가 없으면 systemId를 감사 사용자로 반환한다")
    void unauthenticatedUser() {
        // when
        UUID auditor =
                auditorAware.getCurrentAuditor()
                        .orElseThrow();

        // then
        assertEquals(systemId, auditor);
    }
}