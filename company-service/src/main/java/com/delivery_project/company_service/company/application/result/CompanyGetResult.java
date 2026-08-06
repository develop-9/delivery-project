package com.delivery_project.company_service.company.application.result;

import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.entity.CompanyType;

import java.util.UUID;

public record CompanyGetResult(

        UUID companyId,
        String name,
        CompanyType type,
        UUID hubId,
        String address
) {
    public static CompanyGetResult from(Company company) {
        return new CompanyGetResult(
                company.getId(),
                company.getName(),
                company.getType(),
                company.getHubId(),
                company.getAddress()
        );
    }
}
