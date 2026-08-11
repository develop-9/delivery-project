package com.delivery_project.delivery_service.delivery.application.result;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryRoute;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryRouteStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record DeliveryRouteDetailResult(
        UUID routeId,
        UUID deliveryId,
        Integer sequence,
        UUID departureHubId,
        UUID arrivalHubId,
        BigDecimal estimatedDistanceKm,
        Integer estimatedDurationMin,
        BigDecimal actualDistanceKm,
        Integer actualDurationMin,
        DeliveryRouteStatus status,
        UUID deliveryManagerId
) {
    public static DeliveryRouteDetailResult from(
            DeliveryRoute route
    ){
        return new DeliveryRouteDetailResult(
                route.getId(),
                route.getDeliveryId(),
                route.getSequence(),
                route.getDepartureHubId(),
                route.getArrivalHubId(),
                route.getEstimatedDistanceKm(),
                route.getEstimatedDurationMin(),
                route.getActualDistanceKm(),
                route.getActualDurationMin(),
                route.getStatus(),
                route.getDeliveryManagerId()
        );
    }
}
