package com.delivery_project.delivery_service.delivery.infrastructure.client.dto;

import java.util.List;
import java.util.UUID;

public record RelatedOrderIdsResponse(
        List<UUID> orderIds
) {
}
