package com.delivery_project.company_service.company.presentation.request;

import com.delivery_project.company_service.company.application.command.CompanyGetCommand;

import java.util.UUID;

public record CompanyGetRequest(

) {
    public CompanyGetCommand toCommand(UUID companyId) {
        return new CompanyGetCommand(companyId);
    }
}
