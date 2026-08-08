package com.delivery_project.company_service.company.application.result;

import com.delivery_project.company_service.company.domain.entity.Product;

import java.util.UUID;

public record ProductUpdateResult(

        UUID productId
) {
    public static ProductUpdateResult from(Product product) {
        return new ProductUpdateResult(
                product.getId()
        );
    }
}
