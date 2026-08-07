package com.delivery_project.company_service.company.application.result;

import java.time.Instant;
import java.util.UUID;

public record CompanyDeleteResult(

        UUID companyId,
        Instant deletedAt
) {
    public static CompanyDeleteResult from(UUID companyId, Instant deletedAt) {
        return new CompanyDeleteResult(companyId, deletedAt);
    }
}
