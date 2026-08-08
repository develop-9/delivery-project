package com.delivery_project.delivery_service.delivery.infrastructure.persistence;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryRoute;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryRouteStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataDeliveryRouteRepository
        extends JpaRepository<DeliveryRoute, UUID> {

    List<DeliveryRoute> findAllByDeliveryIdAndStatusAndDeletedAtIsNull(
            UUID deliveryId,
            DeliveryRouteStatus status
    );
}
