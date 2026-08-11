package com.delivery_project.delivery_service.delivery.application.result;

import java.util.UUID;

public record UserAuthorizationInfo(
        UUID userId,
        UUID hubId,
        UUID companyId
) {
}