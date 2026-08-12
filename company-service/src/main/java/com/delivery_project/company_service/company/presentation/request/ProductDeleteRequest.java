package com.delivery_project.company_service.company.presentation.request;

import com.delivery_project.company_service.company.application.command.ProductDeleteCommand;
import com.delivery_project.company_service.global.security.JwtPrincipal;

import java.util.UUID;

public record ProductDeleteRequest(

) {
    public ProductDeleteCommand toCommand(JwtPrincipal jwtPrincipal, UUID productId) {
        return new ProductDeleteCommand(
                jwtPrincipal.userId(),
                productId
        );
    }
}
