package com.delivery_project.company_service.company.infrastructure.client.order;

import com.delivery_project.company_service.company.infrastructure.client.dto.request.InventoryFeignRequest;
import com.delivery_project.company_service.company.infrastructure.client.dto.response.InventoryDeleteFeignResponse;
import com.delivery_project.company_service.company.infrastructure.client.dto.response.InventorySaveFeignResponse;
import com.delivery_project.company_service.global.response.SuccessResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(name = "order-service", path = "/internal/v1/inventories")
public interface OrderClient {

    @PostMapping
    SuccessResponse<InventorySaveFeignResponse> saveInventory(
            @RequestBody InventoryFeignRequest inventoryFeignRequest
    );

    @DeleteMapping("/{inventoryId}")
    SuccessResponse<InventoryDeleteFeignResponse> deleteInventory(
            @PathVariable UUID inventoryId
    );
}
