package com.delivery_project.company_service.company.application.command;

import java.util.UUID;

public record ProductUpdateCommand(

        UUID productId,
        UUID companyId,
        String name,
        Integer price
) {
}
