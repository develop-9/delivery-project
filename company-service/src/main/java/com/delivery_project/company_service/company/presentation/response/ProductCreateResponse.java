package com.delivery_project.company_service.company.presentation.response;

import com.delivery_project.company_service.company.application.result.ProductCreateResult;

import java.util.UUID;

public record ProductCreateResponse(

        UUID productId
) {
    public static ProductCreateResponse from(ProductCreateResult productCreateResult) {
        return new ProductCreateResponse(
                productCreateResult.productId()
        );
    }
}
