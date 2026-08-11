package com.delivery_project.company_service.company.presentation.request;

import com.delivery_project.company_service.company.application.query.ProductSearchQuery;

import java.util.UUID;

public record ProductSearchRequest(

) {
    public ProductSearchQuery toQuery(
            Integer page,
            Integer size,
            String sort,
            UUID companyId,
            String name,
            Integer minPrice,
            Integer maxPrice
    ) {
        return new ProductSearchQuery(
                page,
                size,
                sort,
                companyId,
                name,
                minPrice,
                maxPrice
        );
    }
}
