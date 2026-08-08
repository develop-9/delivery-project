package com.delivery_project.company_service.company.presentation.request;

import com.delivery_project.company_service.company.application.query.InternalCompanyGetQuery;

import java.util.UUID;

public record InternalCompanyGetRequest(

) {
    public InternalCompanyGetQuery toCommand(UUID companyId) {
        return new InternalCompanyGetQuery(companyId);
    }
}
