package com.delivery_project.delivery_service.delivery.domain.repository;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryManagerRepository {

    DeliveryManager save(DeliveryManager deliveryManager);
    boolean existsByUserId(UUID userId);

    Optional<Integer> findMaxSequence(
            UUID hubId,
            DeliveryManagerType type
    );

    Optional<DeliveryManager> findById(UUID managerId);
}