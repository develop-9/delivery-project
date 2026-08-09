package com.delivery_project.company_service.global.security;

import java.util.UUID;

public record JwtPrincipal(

        UUID userId,
        Role role
) {
}
