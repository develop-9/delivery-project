package com.delivery_project.delivery_service.delivery.infrastructure.client.dto;

import java.util.UUID;

public record UserInfoResponse(
        UUID userId,
        String username,
        String name,
        String role,
        UUID hubId,
        UUID companyId
) {
}
