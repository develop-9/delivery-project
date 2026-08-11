package com.delivery_project.delivery_service.delivery.application.result;

import com.delivery_project.delivery_service.delivery.domain.entity.Delivery;
import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryRoute;

import java.util.List;
import java.util.UUID;

public record DeliveryRoutesByOrderResult(
        UUID deliveryId,
        UUID orderId,
        List<DeliveryRouteInternalResult> routes
) {
    public static DeliveryRoutesByOrderResult of(
            Delivery delivery,
            List<DeliveryRoute> routes
    ){
        return new DeliveryRoutesByOrderResult(
                delivery.getId(),
                delivery.getOrderId(),
                routes.stream()
                        .map(DeliveryRouteInternalResult::from)
                        .toList()
        );
    }
}
