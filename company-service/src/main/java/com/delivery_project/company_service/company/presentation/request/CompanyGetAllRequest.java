package com.delivery_project.company_service.company.presentation.request;

import com.delivery_project.company_service.company.application.command.CompanyGetAllCommand;
import com.delivery_project.company_service.company.domain.entity.CompanyType;

import java.util.UUID;

public record CompanyGetAllRequest(

) {
    public CompanyGetAllCommand toCommand(Integer page, Integer size, String sort, String name, CompanyType type, UUID hubId) {
        return new CompanyGetAllCommand(
                page,
                size,
                sort,
                name,
                type,
                hubId
        );
    }
}
