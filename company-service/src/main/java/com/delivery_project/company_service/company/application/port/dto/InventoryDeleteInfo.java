package com.delivery_project.company_service.company.application.port.dto;

import java.time.Instant;
import java.util.UUID;

public record InventoryDeleteInfo(

        UUID inventoryId,
        Integer remainingQuantity,
        Instant deletedAt
) {
}
