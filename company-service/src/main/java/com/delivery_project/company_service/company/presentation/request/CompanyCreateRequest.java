package com.delivery_project.company_service.company.presentation.request;

import com.delivery_project.company_service.company.application.command.CompanyCreateCommand;
import com.delivery_project.company_service.company.domain.entity.CompanyType;
import com.delivery_project.company_service.global.security.JwtPrincipal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CompanyCreateRequest(

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
        public CompanyCreateCommand toCommand(JwtPrincipal jwtPrincipal) {
                return new CompanyCreateCommand(
                        jwtPrincipal.userId(),
                        this.hubId,
                        this.type,
                        this.name,
                        this.address
                );
        }
}
