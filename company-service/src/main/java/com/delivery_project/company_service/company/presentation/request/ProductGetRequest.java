package com.delivery_project.company_service.company.presentation.request;

import com.delivery_project.company_service.company.application.query.ProductGetQuery;
import com.delivery_project.company_service.global.security.JwtPrincipal;

import java.util.UUID;

public record ProductGetRequest(

) {
    public ProductGetQuery toQuery(JwtPrincipal jwtPrincipal, UUID productId) {
        return new ProductGetQuery(
                jwtPrincipal.userId(),
                jwtPrincipal.role(),
                productId
        );
    }
}
