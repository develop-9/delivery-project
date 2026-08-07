package com.delivery_project.delivery_service.delivery.domain.repository;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryRoute;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryRouteStatus;

import java.util.List;
import java.util.UUID;

public interface DeliveryRouteCommandRepository {

    List<DeliveryRoute> saveAll(List<DeliveryRoute> routes);

    List<DeliveryRoute> findAllByDeliveryIdAndStatusAndDeletedAtIsNull(
            UUID deliveryId,
            DeliveryRouteStatus status
    );

}
