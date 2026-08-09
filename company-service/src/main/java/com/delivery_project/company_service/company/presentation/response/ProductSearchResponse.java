package com.delivery_project.company_service.company.presentation.response;

import com.delivery_project.company_service.company.application.result.ProductSearchDataResult;

import java.time.Instant;
import java.util.UUID;

public record ProductSearchResponse(

        UUID productId,
        UUID companyId,
        String name,
        Integer price,
        Instant createdAt
) {
    public static ProductSearchResponse from(ProductSearchDataResult productSearchDataResult) {
        return new ProductSearchResponse(
                productSearchDataResult.productId(),
                productSearchDataResult.companyId(),
                productSearchDataResult.name(),
                productSearchDataResult.price(),
                productSearchDataResult.createdAt()
        );
    }
}
