package com.delivery_project.delivery_service.delivery.domain.entity;

import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;
import com.delivery_project.delivery_service.global.exception.BusinessException;
import com.delivery_project.delivery_service.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DeliveryManagerTest {

    @Test
    @DisplayName("배송 담당자는 생성 시 활성 상태이다")
    void createManagerIsActive() {
        DeliveryManager manager =
                DeliveryManager.create(
                        UUID.randomUUID(),
                        null,
                        DeliveryManagerType.HUB_DELIVERY,
                        0
                );

        assertEquals(true, manager.isActive());
    }

    @Test
    @DisplayName("배송 담당자를 비활성화할 수 있다")
    void deactivateManager() {
        DeliveryManager manager =
                DeliveryManager.create(
                        UUID.randomUUID(),
                        null,
                        DeliveryManagerType.HUB_DELIVERY,
                        0
                );

        manager.deactivate();

        assertEquals(false, manager.isActive());
    }

    @Test
    @DisplayName("비활성화된 배송 담당자는 배정할 수 없다")
    void inactiveManagerCannotBeAssigned() {
        DeliveryManager manager =
                DeliveryManager.create(
                        UUID.randomUUID(),
                        null,
                        DeliveryManagerType.HUB_DELIVERY,
                        0
                );

        manager.deactivate();

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        manager::assignToDelivery
                );

        assertEquals(
                ErrorCode.DELIVERY_MANAGER_NOT_ACTIVE,
                exception.getErrorCode()
        );
    }
}
