package com.delivery_project.company_service.company.application.query;

import com.delivery_project.company_service.company.domain.entity.CompanyType;

import java.util.UUID;

public record CompanySearchQuery(

        Integer page,
        Integer size,
        String sort,
        String name,
        CompanyType type,
        UUID hubId
) {
}
