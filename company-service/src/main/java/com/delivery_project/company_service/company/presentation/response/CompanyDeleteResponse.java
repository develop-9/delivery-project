package com.delivery_project.company_service.company.presentation.response;

import com.delivery_project.company_service.company.application.result.CompanyDeleteResult;

import java.time.Instant;
import java.util.UUID;

public record CompanyDeleteResponse(

        UUID companyId,
        Instant deletedAt
) {

    public static CompanyDeleteResponse from(CompanyDeleteResult companyDeleteResult) {
        return new CompanyDeleteResponse(
                companyDeleteResult.companyId(),
                companyDeleteResult.deletedAt()
        );
    }
}
