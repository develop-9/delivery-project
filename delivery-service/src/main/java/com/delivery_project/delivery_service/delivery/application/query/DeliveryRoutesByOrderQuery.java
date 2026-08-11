package com.delivery_project.delivery_service.delivery.application.query;

import java.util.UUID;

public record DeliveryRoutesByOrderQuery(
        UUID orderId
) {
    public static DeliveryRoutesByOrderQuery from(
            UUID orderId
    ) {
        return new DeliveryRoutesByOrderQuery(orderId);
    }
}