package com.delivery_project.company_service.company.presentation.request;

import com.delivery_project.company_service.company.application.command.ProductDeleteCommand;

import java.util.UUID;

public record ProductDeleteRequest(

) {
    public ProductDeleteCommand toCommand(UUID productId) {
        return new ProductDeleteCommand(productId);
    }
}
