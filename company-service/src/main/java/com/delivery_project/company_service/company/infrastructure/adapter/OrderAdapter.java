package com.delivery_project.company_service.company.infrastructure.adapter;

import com.delivery_project.company_service.company.application.port.OrderPort;
import com.delivery_project.company_service.company.application.port.dto.InventoryDeleteInfo;
import com.delivery_project.company_service.company.application.port.dto.InventorySaveInfo;
import com.delivery_project.company_service.company.infrastructure.client.dto.request.InventoryFeignRequest;
import com.delivery_project.company_service.company.infrastructure.client.dto.response.InventoryDeleteFeignResponse;
import com.delivery_project.company_service.company.infrastructure.client.dto.response.InventorySaveFeignResponse;
import com.delivery_project.company_service.company.infrastructure.client.order.OrderClient;
import com.delivery_project.company_service.global.exception.BusinessException;
import com.delivery_project.company_service.global.exception.ErrorCode;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderAdapter implements OrderPort {

    private final OrderClient orderClient;

    @Override
    public List<InventorySaveInfo> saveInventory(UUID productId) {

        try {
            InventorySaveFeignResponse inventorySaveFeignResponse
                    = orderClient.saveInventory(new InventoryFeignRequest(productId)).data();

            return inventorySaveFeignResponse.inventoryList();

        } catch (FeignException.NotFound e) {
            log.warn(
                    "재고 저장 실패. productId={}",
                    productId
            );

            throw new BusinessException(
                    ErrorCode.INVENTORY_SAVE_FAILED
            );
        } catch (FeignException e) {
            log.error(
                    "Order Service 호출 실패. productId={}, status={}",
                    productId,
                    e.status(),
                    e
            );

            throw new BusinessException(
                    ErrorCode.ORDER_SERVICE_UNAVAILABLE
            );
        }
    }

    @Override
    public List<InventoryDeleteInfo> deleteInventory(UUID productId) {

        try {
            InventoryDeleteFeignResponse inventoryDeleteFeignResponse
                    = orderClient.deleteInventory(productId).data();

            return inventoryDeleteFeignResponse.inventoryList();

        } catch (FeignException e) {
            log.error(
                    "Order Service 호출 실패. productId={}, status={}",
                    productId,
                    e.status(),
                    e
            );

            throw new BusinessException(
                    ErrorCode.ORDER_SERVICE_UNAVAILABLE
            );
        }
    }
}
