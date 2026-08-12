package com.delivery_project.company_service.company.application.port.dto;

import com.delivery_project.company_service.global.security.Role;

import java.util.UUID;

public record CallerInfo(

        UUID userId,
        Role role,
        UUID hubId,
        UUID companyId
) {
}
