package com.delivery_project.company_service.company.application.result;

import com.delivery_project.company_service.company.domain.entity.Product;

import java.util.UUID;

public record ProductGetResult(

        UUID productId,
        UUID companyId,
        String name,
        Integer price
) {
    public static ProductGetResult from(Product product) {
        return new ProductGetResult(
                product.getId(),
                product.getCompanyId(),
                product.getName(),
                product.getPrice()
        );
    }
}
