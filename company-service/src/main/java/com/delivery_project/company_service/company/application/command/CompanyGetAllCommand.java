package com.delivery_project.company_service.company.application.command;

import com.delivery_project.company_service.company.domain.entity.CompanyType;

import java.util.UUID;

public record CompanyGetAllCommand(

        Integer page,
        Integer size,
        String sort,
        String name,
        CompanyType type,
        UUID hubId
) {
}
