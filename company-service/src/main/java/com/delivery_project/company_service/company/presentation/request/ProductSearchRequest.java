package com.delivery_project.company_service.company.presentation.request;

import com.delivery_project.company_service.company.application.query.ProductSearchQuery;
import com.delivery_project.company_service.global.security.JwtPrincipal;

import java.util.UUID;

public record ProductSearchRequest(

) {
    public ProductSearchQuery toQuery(
            JwtPrincipal jwtPrincipal,
            Integer page,
            Integer size,
            String sort,
            UUID companyId,
            String name,
            Integer minPrice,
            Integer maxPrice
    ) {
        return new ProductSearchQuery(
                jwtPrincipal.userId(),
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
