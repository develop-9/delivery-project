package com.delivery_project.company_service.company.presentation.response;

import com.delivery_project.company_service.company.application.result.ProductDeleteResult;

import java.time.Instant;
import java.util.UUID;

public record ProductDeleteResponse(

        UUID productId,
        Instant deletedAt
) {
    public static ProductDeleteResponse from(ProductDeleteResult productDeleteResult) {
        return new ProductDeleteResponse(
                productDeleteResult.productId(),
                productDeleteResult.deletedAt()
        );
    }
}
