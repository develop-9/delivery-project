package com.delivery_project.company_service.company.presentation.response;

import com.delivery_project.company_service.company.application.result.CompanyUpdateResult;

import java.util.UUID;

public record CompanyUpdateResponse(

        UUID companyId
) {
    public static CompanyUpdateResponse from(CompanyUpdateResult companyUpdateResult) {
        return new CompanyUpdateResponse(companyUpdateResult.companyId());
    }
}
