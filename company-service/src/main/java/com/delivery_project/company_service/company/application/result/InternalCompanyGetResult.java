package com.delivery_project.company_service.company.application.result;

import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.entity.CompanyType;

import java.util.UUID;

public record InternalCompanyGetResult(

        UUID companyId,
        String name,
        CompanyType type
) {
    public static InternalCompanyGetResult from(Company company) {
        return new InternalCompanyGetResult(
                company.getId(),
                company.getName(),
                company.getType()
        );
    }
}
