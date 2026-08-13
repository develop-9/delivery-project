package com.delivery_project.company_service.company.presentation.response;

import com.delivery_project.company_service.company.application.result.InternalCompanyGetResult;
import com.delivery_project.company_service.company.domain.entity.CompanyType;

import java.util.UUID;

public record InternalCompanyGetResponse(

        UUID companyId,
        String name,
        CompanyType type,
        UUID hubId,
        String address
) {
    public static InternalCompanyGetResponse from(InternalCompanyGetResult internalCompanyGetResult) {
        return new InternalCompanyGetResponse(
                internalCompanyGetResult.companyId(),
                internalCompanyGetResult.name(),
                internalCompanyGetResult.type(),
                internalCompanyGetResult.hubId(),
                internalCompanyGetResult.address()
        );
    }
}
