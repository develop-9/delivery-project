package com.delivery_project.company_service.company.presentation.request;

import com.delivery_project.company_service.company.application.query.CompanyGetQuery;

import java.util.UUID;

public record CompanyGetRequest(

) {
    public CompanyGetQuery toCommand(UUID companyId) {
        return new CompanyGetQuery(companyId);
    }
}
