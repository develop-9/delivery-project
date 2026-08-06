package com.delivery_project.company_service.company.application.result;

import java.util.UUID;

public record CompanyUpdateResult(

        UUID companyId
) {
    public static CompanyUpdateResult from(UUID companyId) {
        return new CompanyUpdateResult(companyId);
    }
}
