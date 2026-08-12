package com.delivery_project.company_service.company.infrastructure.client.dto.response;

import com.delivery_project.company_service.company.application.port.dto.InventorySaveInfo;

import java.util.List;

public record InventorySaveFeignResponse(

        List<InventorySaveInfo> inventoryList
) {
}
