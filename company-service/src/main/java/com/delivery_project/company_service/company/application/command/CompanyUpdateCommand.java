package com.delivery_project.company_service.company.application.command;

import com.delivery_project.company_service.company.domain.entity.CompanyType;
import com.delivery_project.company_service.company.presentation.request.CompanyUpdateRequest;

import java.util.UUID;

public record CompanyUpdateCommand(

        UUID hubId,
        CompanyType type,
        String name,
        String address
) {
    public static CompanyUpdateCommand from(CompanyUpdateRequest companyUpdateRequest) {
        return new CompanyUpdateCommand(
                companyUpdateRequest.hubId(),
                companyUpdateRequest.type(),
                companyUpdateRequest.name(),
                companyUpdateRequest.address()
        );
    }
}
