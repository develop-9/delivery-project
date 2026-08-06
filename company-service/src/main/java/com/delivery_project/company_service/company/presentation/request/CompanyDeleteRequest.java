package com.delivery_project.company_service.company.presentation.request;

import com.delivery_project.company_service.company.application.command.CompanyDeleteCommand;

import java.util.UUID;

public record CompanyDeleteRequest(

) {
    public CompanyDeleteCommand toCommand(UUID companyId) {
        return new CompanyDeleteCommand(companyId);
    }
}
