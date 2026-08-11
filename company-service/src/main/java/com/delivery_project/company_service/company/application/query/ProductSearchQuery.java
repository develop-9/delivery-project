package com.delivery_project.company_service.company.application.query;

import java.util.UUID;

public record ProductSearchQuery(

        Integer page,
        Integer size,
        String sort,
        UUID companyId,
        String name,
        Integer minPrice,
        Integer maxPrice
) {
}
