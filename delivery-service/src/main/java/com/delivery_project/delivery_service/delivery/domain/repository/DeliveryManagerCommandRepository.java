package com.delivery_project.delivery_service.delivery.domain.repository;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryManagerCommandRepository {

    DeliveryManager save(DeliveryManager deliveryManager);

    boolean existsByUserId(UUID userId);

    Optional<DeliveryManager> findById(UUID managerId);

    Optional<DeliveryManager> findByUserId(UUID userId);

    Optional<Integer> findMaxSequenceByType(
            DeliveryManagerType type
    );

    Optional<Integer> findMaxSequenceByHubIdAndType(
            UUID hubId,
            DeliveryManagerType type
    );
}