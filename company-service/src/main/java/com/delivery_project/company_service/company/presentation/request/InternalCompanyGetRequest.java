package com.delivery_project.company_service.company.presentation.request;

import com.delivery_project.company_service.company.application.command.InternalCompanyGetCommand;

import java.util.UUID;

public record InternalCompanyGetRequest(

) {
    public InternalCompanyGetCommand toCommand(UUID companyId) {
        return new InternalCompanyGetCommand(companyId);
    }
}
