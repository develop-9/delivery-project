package com.delivery_project.company_service.company.presentation.request;

import com.delivery_project.company_service.company.application.command.ProductCreateCommand;
import jakarta.validation.constraints.*;

import java.util.UUID;

public record ProductCreateRequest(

        @NotNull
        UUID companyId,

        @NotBlank
        @Size(max = 100)
        String name,

        @NotNull
        @Positive
        Integer price
) {
    public ProductCreateCommand toCommand() {
        return new ProductCreateCommand(companyId, name, price);
    }
}
