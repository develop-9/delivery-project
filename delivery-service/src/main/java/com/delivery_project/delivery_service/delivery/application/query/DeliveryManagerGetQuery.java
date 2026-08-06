package com.delivery_project.delivery_service.delivery.application.query;

import java.util.UUID;

public record DeliveryManagerGetQuery(
        UUID managerId
) {

    public static DeliveryManagerGetQuery from(
            UUID managerId
    ) {
        return new DeliveryManagerGetQuery(managerId);
    }
}