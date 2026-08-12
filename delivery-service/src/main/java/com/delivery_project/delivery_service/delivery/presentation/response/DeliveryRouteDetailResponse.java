package com.delivery_project.delivery_service.delivery.presentation.response;

import com.delivery_project.delivery_service.delivery.application.result.DeliveryRouteDetailResult;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryRouteStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record DeliveryRouteDetailResponse(
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
    public static DeliveryRouteDetailResponse from(
            DeliveryRouteDetailResult result
    ) {
        return new DeliveryRouteDetailResponse(
                result.routeId(),
                result.deliveryId(),
                result.sequence(),
                result.departureHubId(),
                result.arrivalHubId(),
                result.estimatedDistanceKm(),
                result.estimatedDurationMin(),
                result.actualDistanceKm(),
                result.actualDurationMin(),
                result.status(),
                result.deliveryManagerId()
        );
    }
}
