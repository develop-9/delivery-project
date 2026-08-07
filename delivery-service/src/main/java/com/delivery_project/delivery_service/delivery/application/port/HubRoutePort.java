package com.delivery_project.delivery_service.delivery.application.port;

import com.delivery_project.delivery_service.delivery.application.result.DeliveryPath;

import java.util.UUID;

public interface HubRoutePort {

    DeliveryPath getDeliveryPath(
            UUID departureHubId,
            UUID destinationHubId
    );
}
