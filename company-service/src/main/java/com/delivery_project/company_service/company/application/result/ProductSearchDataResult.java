package com.delivery_project.company_service.company.application.result;

import com.delivery_project.company_service.company.domain.entity.Product;

import java.time.Instant;
import java.util.UUID;

public record ProductSearchDataResult(

        UUID productId,
        UUID companyId,
        String name,
        Integer price,
        Instant createdAt
) {
    public static ProductSearchDataResult from(Product product) {
        return new ProductSearchDataResult(
                product.getId(),
                product.getCompanyId(),
                product.getName(),
                product.getPrice(),
                product.getCreatedAt()
        );
    }
}
