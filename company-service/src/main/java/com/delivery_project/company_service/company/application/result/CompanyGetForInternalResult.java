package com.delivery_project.company_service.company.application.result;

import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.entity.CompanyType;

import java.util.UUID;

public record CompanyGetForInternalResult(

        UUID companyId,
        String name,
        CompanyType type
) {
    public static CompanyGetForInternalResult from(Company company) {
        return new CompanyGetForInternalResult(
                company.getId(),
                company.getName(),
                company.getType()
        );
    }
}
