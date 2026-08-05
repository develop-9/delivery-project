package com.delivery_project.company_service.company.presentation.response;

import com.delivery_project.company_service.company.application.result.CompanyGetAllDataResult;
import com.delivery_project.company_service.company.domain.entity.CompanyType;

import java.time.Instant;
import java.util.UUID;

public record CompanyGetAllDataResponse(

        UUID companyId,
        String companyName,
        CompanyType companyType,
        UUID hubId,
        String companyAddress,
        Instant createdAt
) {
    public static CompanyGetAllDataResponse from(CompanyGetAllDataResult companyGetAllDataResult) {
        return new CompanyGetAllDataResponse(
                companyGetAllDataResult.companyId(),
                companyGetAllDataResult.companyName(),
                companyGetAllDataResult.companyType(),
                companyGetAllDataResult.hubId(),
                companyGetAllDataResult.companyAddress(),
                companyGetAllDataResult.createdAt()
        );
    }
}
