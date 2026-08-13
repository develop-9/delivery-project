package com.delivery_project.delivery_service.delivery.application.result;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;

import java.util.UUID;

public record DeliveryManagerCreateResult(
        UUID managerId
) {

    public static DeliveryManagerCreateResult from(
            DeliveryManager deliveryManager
    ){
        return new DeliveryManagerCreateResult(
                deliveryManager.getId()
        );
    }
}
