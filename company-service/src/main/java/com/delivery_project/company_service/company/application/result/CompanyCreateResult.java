package com.delivery_project.company_service.company.application.result;

import java.util.UUID;

public record CompanyCreateResult(

        UUID companyId
) {
    public static CompanyCreateResult from(UUID companyId) {
        return new CompanyCreateResult(companyId);
    }
}
