package com.delivery_project.company_service.company.application.command;

import java.util.UUID;

public record CompanyGetCommand(

        UUID companyId
) {
    public static CompanyGetCommand from(UUID companyId) {
        return new CompanyGetCommand(companyId);
    }
}
