package com.delivery_project.delivery_service.delivery.domain.repository;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryRoute;

import java.util.List;
import java.util.UUID;

public interface DeliveryRouteQueryRepository {

    List<DeliveryRoute> findAllByDeliveryIdOrderBySequenceAsc(
            UUID deliveryId
    );
}
