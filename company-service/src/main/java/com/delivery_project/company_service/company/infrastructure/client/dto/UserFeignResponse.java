package com.delivery_project.company_service.company.infrastructure.client.dto;

import com.delivery_project.company_service.global.security.Role;

import java.util.UUID;

public record UserFeignResponse(

        UUID userId,
        String username,
        String name,
        Role role,
        UUID hubId,
        UUID companyId
) {
}
