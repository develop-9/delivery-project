package com.delivery_project.company_service.company.application.result;

import com.delivery_project.company_service.company.domain.entity.Product;

import java.util.UUID;

public record ProductCreateResult(
        UUID productId
) {
    public static ProductCreateResult from(Product product) {
        return new ProductCreateResult(
                product.getId()
        );
    }
}
