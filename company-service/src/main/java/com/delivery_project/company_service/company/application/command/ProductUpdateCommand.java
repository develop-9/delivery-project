package com.delivery_project.company_service.company.application.command;

import com.delivery_project.company_service.global.security.Role;

import java.util.UUID;

public record ProductUpdateCommand(

        UUID callerId,
        UUID productId,
        UUID companyId,
        String name,
        Integer price
) {
}
