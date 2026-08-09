package com.delivery_project.company_service.company.presentation.request;

import com.delivery_project.company_service.company.application.query.InternalProductGetQuery;

import java.util.UUID;

public record InternalProductGetRequest(

) {
    public InternalProductGetQuery toQuery(UUID productId) {
        return new InternalProductGetQuery(productId);
    }
}
