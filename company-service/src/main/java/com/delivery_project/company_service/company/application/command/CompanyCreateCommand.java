package com.delivery_project.company_service.company.application.command;

import com.delivery_project.company_service.company.domain.entity.CompanyType;
import com.delivery_project.company_service.company.presentation.request.CompanyCreateRequest;

import java.util.UUID;

public record CompanyCreateCommand(

        UUID hubId,
        CompanyType type,
        String name,
        String address
) {
    public static CompanyCreateCommand from(CompanyCreateRequest companyCreateRequest) {
        return new CompanyCreateCommand(
                companyCreateRequest.hubId(),
                companyCreateRequest.type(),
                companyCreateRequest.name(),
                companyCreateRequest.address()
        );
    }
}
