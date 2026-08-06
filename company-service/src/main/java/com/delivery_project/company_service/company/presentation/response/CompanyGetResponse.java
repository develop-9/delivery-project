package com.delivery_project.company_service.company.presentation.response;

import com.delivery_project.company_service.company.application.result.CompanyGetResult;

import java.util.UUID;

public record CompanyGetResponse(

        UUID companyId,
        String name,
        String type,
        UUID hubId,
        String address
) {
    public static CompanyGetResponse from(CompanyGetResult companyGetResult) {
        return new CompanyGetResponse(
                companyGetResult.companyId(),
                companyGetResult.name(),
                companyGetResult.type().name(),
                companyGetResult.hubId(),
                companyGetResult.address()
        );
    }
}
