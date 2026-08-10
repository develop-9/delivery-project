package com.delivery_project.company_service.company.infrastructure.client.dto.response;

import com.delivery_project.company_service.company.application.port.dto.InventoryDeleteInfo;

import java.util.List;

public record InventoryDeleteFeignResponse(

        List<InventoryDeleteInfo> inventoryList
) {
}
