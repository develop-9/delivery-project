package com.delivery_project.delivery_service.delivery.presentation.response;

import com.delivery_project.delivery_service.delivery.application.result.DeliveryRoutesByOrderResult;

import java.util.List;
import java.util.UUID;

public record DeliveryRoutesByOrderResponse(
        UUID deliveryId,
        UUID orderId,
        List<DeliveryRouteInternalResponse> routes
) {
    public static DeliveryRoutesByOrderResponse from(
            DeliveryRoutesByOrderResult result
    ){
        return new DeliveryRoutesByOrderResponse(
                result.deliveryId(),
                result.orderId(),
                result.routes()
                        .stream()
                        .map(DeliveryRouteInternalResponse::from)
                        .toList()
        );
    }
}
