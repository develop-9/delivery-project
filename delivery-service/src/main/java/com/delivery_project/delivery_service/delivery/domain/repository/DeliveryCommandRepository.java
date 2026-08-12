package com.delivery_project.delivery_service.delivery.domain.repository;

import com.delivery_project.delivery_service.delivery.domain.entity.Delivery;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryCommandRepository {

    Delivery save(Delivery delivery);

    Optional<Delivery> findByOrderIdAndDeletedAtIsNull(UUID orderId);

    Optional<Delivery> findById(UUID deliveryId);

    boolean existsActiveByCompanyDeliveryManagerId(
            UUID managerId
    );
}