package com.delivery_project.delivery_service.delivery.presentation.response;

import com.delivery_project.delivery_service.delivery.application.result.DeliveryRouteInternalResult;

import java.util.UUID;

public record DeliveryRouteInternalResponse(
        Integer sequence,
        UUID departureHubId,
        UUID arrivalHubId,
        Integer estimatedDurationMin
) {
    public static DeliveryRouteInternalResponse from(
            DeliveryRouteInternalResult result
    ){
        return new DeliveryRouteInternalResponse(
                result.sequence(),
                result.departureHubId(),
                result.arrivalHubId(),
                result.estimatedDurationMin()
        );
    }
}
