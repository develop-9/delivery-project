package com.delivery_project.delivery_service.delivery.domain.repository;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryManagerCommandRepository {

    DeliveryManager save(DeliveryManager deliveryManager);

    boolean existsByUserId(UUID userId);

    Optional<DeliveryManager> findById(UUID managerId);

    Optional<DeliveryManager> findByUserId(UUID userId);

}