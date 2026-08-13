package com.delivery_project.company_service.company.application.command;

import com.delivery_project.company_service.company.domain.entity.CompanyType;
import com.delivery_project.company_service.global.security.Role;

import java.util.UUID;

public record CompanyCreateCommand(

        UUID callerId,
        UUID hubId,
        CompanyType type,
        String name,
        String address
) {
}
