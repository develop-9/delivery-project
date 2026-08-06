package com.delivery_project.company_service.company.presentation.response;

import com.delivery_project.company_service.company.application.result.ProductUpdateResult;

import java.util.UUID;

public record ProductUpdateResponse(

        UUID productId
) {
    public static ProductUpdateResponse from(ProductUpdateResult productUpdateResult) {
        return new ProductUpdateResponse(
                productUpdateResult.productId()
        );
    }
}
