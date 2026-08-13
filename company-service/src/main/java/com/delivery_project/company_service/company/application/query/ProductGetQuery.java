package com.delivery_project.company_service.company.application.query;

import com.delivery_project.company_service.global.security.Role;

import java.util.UUID;

public record ProductGetQuery(

        UUID callerId,
        UUID productId
) {
}
