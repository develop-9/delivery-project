package com.delivery_project.company_service.company.application.result;

import com.delivery_project.company_service.company.domain.entity.Product;

import java.util.UUID;

public record InternalProductGetResult(

        UUID productId,
        String name,
        Integer price
) {
    public static InternalProductGetResult from(Product product) {
        return new InternalProductGetResult(
                product.getId(),
                product.getName(),
                product.getPrice()
        );
    }
}
