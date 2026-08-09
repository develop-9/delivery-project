package com.delivery_project.company_service.company.presentation.request;

import com.delivery_project.company_service.company.application.query.CompanyGetQuery;
import com.delivery_project.company_service.global.security.JwtPrincipal;

import java.util.UUID;

public record CompanyGetRequest(

) {
    public CompanyGetQuery toQuery(JwtPrincipal jwtPrincipal, UUID companyId) {
        return new CompanyGetQuery(
                jwtPrincipal.userId(),
                jwtPrincipal.role(),
                companyId
        );
    }
}
