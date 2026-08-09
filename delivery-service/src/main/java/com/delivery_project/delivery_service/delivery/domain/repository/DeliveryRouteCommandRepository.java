package com.delivery_project.delivery_service.delivery.domain.repository;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryRoute;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryRouteStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryRouteCommandRepository {

    DeliveryRoute save(DeliveryRoute route);

    List<DeliveryRoute> saveAll(List<DeliveryRoute> routes);

    Optional<DeliveryRoute> findById(UUID routeId);

    List<DeliveryRoute> findAllByDeliveryIdAndStatusAndDeletedAtIsNull(
            UUID deliveryId,
            DeliveryRouteStatus status
    );

    Optional<DeliveryRoute> findByDeliveryIdAndSequenceAndDeletedAtIsNull(
            UUID deliveryId,
            Integer sequence
    );

    Optional<DeliveryRoute> findLastByDeliveryId(UUID deliveryId);

    List<DeliveryRoute> findAllByDeliveryIdAndDeletedAtIsNull(
            UUID deliveryId
    );
}
