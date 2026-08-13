package com.delivery_project.company_service.company.presentation.request;

import com.delivery_project.company_service.company.application.command.ProductUpdateCommand;
import com.delivery_project.company_service.global.security.JwtPrincipal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ProductUpdateRequest(

        @NotNull
        UUID companyId,

        @NotBlank
        @Size(max = 100)
        String name,

        @NotNull
        @Positive
        Integer price
) {
    public ProductUpdateCommand toCommand(JwtPrincipal jwtPrincipal, UUID productId) {
        return new ProductUpdateCommand(
                jwtPrincipal.userId(),
                productId,
                companyId,
                name,
                price
        );
    }
}
