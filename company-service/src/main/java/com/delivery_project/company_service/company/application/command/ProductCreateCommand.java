package com.delivery_project.company_service.company.application.command;

import java.util.UUID;

public record ProductCreateCommand(

        UUID companyId,
        String name,
        Integer price
) {
}
