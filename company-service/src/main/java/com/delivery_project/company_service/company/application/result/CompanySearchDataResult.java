package com.delivery_project.company_service.company.application.result;

import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.entity.CompanyType;

import java.time.Instant;
import java.util.UUID;

public record CompanySearchDataResult(

        UUID companyId,
        String companyName,
        CompanyType companyType,
        UUID hubId,
        String companyAddress,
        Instant createdAt
) {
    public static CompanySearchDataResult from(Company company) {
        return new CompanySearchDataResult(
                company.getId(),
                company.getName(),
                company.getType(),
                company.getHubId(),
                company.getAddress(),
                company.getCreatedAt()
        );
    }
}
