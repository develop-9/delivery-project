package com.delivery_project.delivery_service.delivery.application.result;

import java.math.BigDecimal;
import java.util.UUID;

public record DeliveryPathSegment(
        Integer sequence,
        UUID departureHubId,
        UUID arrivalHubId,
        BigDecimal distanceKm,
        Integer durationMin
) {
}
