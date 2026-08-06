package com.delivery_project.company_service.company.application.command;

import java.util.UUID;

public record InternalCompanyGetCommand(

        UUID companyId
) {
}
