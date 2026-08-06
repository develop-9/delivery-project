package com.delivery_project.company_service.company.presentation.response;

import com.delivery_project.company_service.company.application.result.CompanyCreateResult;

import java.util.UUID;

public record CompanyCreateResponse(

        UUID companyId
) {
    public static CompanyCreateResponse from(CompanyCreateResult companyCreateResult) {
        return new CompanyCreateResponse(companyCreateResult.companyId());
    }
}
