package com.delivery_project.company_service.company.presentation.request;

import com.delivery_project.company_service.company.application.query.ProductGetQuery;

import java.util.UUID;

public record ProductGetRequest(

) {
    public ProductGetQuery toQuery(UUID productId) {
        return new ProductGetQuery(productId);
    }
}
