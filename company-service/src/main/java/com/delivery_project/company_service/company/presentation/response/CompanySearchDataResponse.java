package com.delivery_project.company_service.company.presentation.response;

import com.delivery_project.company_service.company.application.result.CompanySearchDataResult;
import com.delivery_project.company_service.company.domain.entity.CompanyType;

import java.time.Instant;
import java.util.UUID;

public record CompanySearchDataResponse(

        UUID companyId,
        String companyName,
        CompanyType companyType,
        UUID hubId,
        String companyAddress,
        Instant createdAt
) {
    public static CompanySearchDataResponse from(CompanySearchDataResult companySearchDataResult) {
        return new CompanySearchDataResponse(
                companySearchDataResult.companyId(),
                companySearchDataResult.companyName(),
                companySearchDataResult.companyType(),
                companySearchDataResult.hubId(),
                companySearchDataResult.companyAddress(),
                companySearchDataResult.createdAt()
        );
    }
}
