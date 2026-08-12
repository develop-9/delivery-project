package com.delivery_project.company_service.company.application.query;

import com.delivery_project.company_service.global.security.Role;

import java.util.UUID;

public record ProductSearchQuery(

        UUID callerId,
        Integer page,
        Integer size,
        String sort,
        UUID companyId,
        String name,
        Integer minPrice,
        Integer maxPrice
) {
}
