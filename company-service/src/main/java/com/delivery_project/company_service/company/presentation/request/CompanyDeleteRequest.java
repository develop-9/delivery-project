package com.delivery_project.company_service.company.presentation.request;

import com.delivery_project.company_service.company.application.command.CompanyDeleteCommand;
import com.delivery_project.company_service.global.security.JwtPrincipal;

import java.util.UUID;

public record CompanyDeleteRequest(

) {
    public CompanyDeleteCommand toCommand(JwtPrincipal jwtPrincipal, UUID companyId) {
        return new CompanyDeleteCommand(
                jwtPrincipal.userId(),
                jwtPrincipal.role(),
                companyId
        );
    }
}
