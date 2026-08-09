package com.delivery_project.company_service.company.presentation.request;

import com.delivery_project.company_service.company.application.query.CompanySearchQuery;
import com.delivery_project.company_service.company.domain.entity.CompanyType;
import com.delivery_project.company_service.global.security.JwtPrincipal;

import java.util.UUID;

public record CompanySearchRequest(

) {
    public CompanySearchQuery toQuery(JwtPrincipal jwtPrincipal, Integer page, Integer size, String sort, String name, CompanyType type, UUID hubId) {
        return new CompanySearchQuery(
                jwtPrincipal.userId(),
                jwtPrincipal.role(),
                page,
                size,
                sort,
                name,
                type,
                hubId
        );
    }
}
