package com.delivery_project.company_service.company.application.port;

import com.delivery_project.company_service.company.application.port.dto.InventoryDeleteInfo;
import com.delivery_project.company_service.company.application.port.dto.InventorySaveInfo;

import java.util.List;
import java.util.UUID;

public interface OrderPort {

    List<InventorySaveInfo> saveInventory(UUID productId);

    List<InventoryDeleteInfo> deleteInventory(UUID productId);
}
