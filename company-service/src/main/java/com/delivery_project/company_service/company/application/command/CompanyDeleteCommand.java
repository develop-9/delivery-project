package com.delivery_project.company_service.company.application.command;

import java.util.UUID;

public record CompanyDeleteCommand(

        UUID companyId
) {
    public static CompanyDeleteCommand from(UUID companyId) {
        return new CompanyDeleteCommand(companyId);
    }
}
