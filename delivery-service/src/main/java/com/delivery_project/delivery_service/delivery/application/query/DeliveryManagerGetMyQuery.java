package com.delivery_project.delivery_service.delivery.application.query;

import java.util.UUID;

public record DeliveryManagerGetMyQuery(
        UUID userId
) {

    public static DeliveryManagerGetMyQuery from(
            UUID userId
    ) {
        return new DeliveryManagerGetMyQuery(userId);
    }
}