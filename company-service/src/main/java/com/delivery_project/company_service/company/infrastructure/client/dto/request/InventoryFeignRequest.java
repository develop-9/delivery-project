package com.delivery_project.company_service.company.infrastructure.client.dto.request;

import java.util.UUID;

public record InventoryFeignRequest(

        UUID productId
) {
}
