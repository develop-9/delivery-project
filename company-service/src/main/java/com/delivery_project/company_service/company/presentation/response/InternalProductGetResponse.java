package com.delivery_project.company_service.company.presentation.response;

import com.delivery_project.company_service.company.application.result.InternalProductGetResult;

import java.util.UUID;

public record InternalProductGetResponse(

        UUID productId,
        String name,
        Integer price
) {
    public static InternalProductGetResponse from(InternalProductGetResult internalProductGetResult) {
        return new InternalProductGetResponse(
                internalProductGetResult.productId(),
                internalProductGetResult.name(),
                internalProductGetResult.price()
        );
    }
}
