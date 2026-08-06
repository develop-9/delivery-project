package com.delivery_project.company_service.company.presentation.response;

import com.delivery_project.company_service.company.application.result.CompanyGetForInternalResult;
import com.delivery_project.company_service.company.domain.entity.CompanyType;

import java.util.UUID;

public record CompanyGetForInternalResponse(

        UUID companyId,
        String name,
        CompanyType type
) {
    public static CompanyGetForInternalResponse from(CompanyGetForInternalResult companyGetForInternalResult) {
        return new CompanyGetForInternalResponse(
                companyGetForInternalResult.companyId(),
                companyGetForInternalResult.name(),
                companyGetForInternalResult.type()
        );
    }
}
