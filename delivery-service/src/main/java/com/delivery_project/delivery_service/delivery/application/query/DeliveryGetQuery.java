package com.delivery_project.delivery_service.delivery.application.query;

import java.util.UUID;

public record DeliveryGetQuery(
        UUID deliveryId
) {
    public static DeliveryGetQuery from(
            UUID deliveryId
    ){
        return new DeliveryGetQuery(deliveryId);
    }
}
