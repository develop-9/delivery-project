package com.delivery_project.company_service.company.application.result;

import com.delivery_project.company_service.company.domain.entity.Product;

import java.time.Instant;
import java.util.UUID;

public record ProductDeleteResult(

        UUID productId,
        Instant deletedAt
) {
    public static ProductDeleteResult from(Product product) {
        return new ProductDeleteResult(
                product.getId(),
                product.getDeletedAt()
        );
    }
}
