package com.delivery_project.company_service.company.presentation.request;

import com.delivery_project.company_service.company.application.command.CompanyUpdateCommand;
import com.delivery_project.company_service.company.domain.entity.CompanyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CompanyUpdateRequest(

        @NotNull
        UUID hubId,

        @NotNull
        CompanyType type,

        @NotBlank
        @Size(max = 100)
        String name,

        @NotBlank
        @Size(max = 255)
        String address
) {
        public CompanyUpdateCommand toCommand(UUID companyId) {
                return new CompanyUpdateCommand(
                        companyId,
                        this.hubId,
                        this.type,
                        this.name,
                        this.address
                );
        }
}
