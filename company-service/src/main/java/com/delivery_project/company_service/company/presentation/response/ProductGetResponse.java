package com.delivery_project.company_service.company.presentation.response;

import com.delivery_project.company_service.company.application.result.ProductGetResult;

import java.util.UUID;

public record ProductGetResponse(

        UUID productId,
        UUID companyId,
        String name,
        Integer price
) {
    public static ProductGetResponse from(ProductGetResult productGetResult) {
        return new ProductGetResponse(
                productGetResult.productId(),
                productGetResult.companyId(),
                productGetResult.name(),
                productGetResult.price()
        );
    }
}
