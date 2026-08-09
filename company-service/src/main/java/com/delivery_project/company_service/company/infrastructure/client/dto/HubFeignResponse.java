package com.delivery_project.company_service.company.infrastructure.client.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record HubFeignResponse(

        UUID hubId,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        HubType hubType,
        UUID parentHubId
) {
}
