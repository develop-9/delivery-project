package com.delivery_project.delivery_service.delivery.application.result;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryRoute;

import java.util.UUID;

public record DeliveryRouteInternalResult(
        Integer sequence,
        UUID departureHubId,
        UUID arrivalHubId,
        Integer estimatedDurationMin
) {
    public static DeliveryRouteInternalResult from(
            DeliveryRoute route
    ){
        return new DeliveryRouteInternalResult(
                route.getSequence(),
                route.getDepartureHubId(),
                route.getArrivalHubId(),
                route.getEstimatedDurationMin()
        );
    }
}
