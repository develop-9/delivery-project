package com.delivery_project.delivery_service.delivery.domain.repository;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryManagerRepository {

    DeliveryManager save(DeliveryManager deliveryManager);
    boolean existsByUserId(UUID userId);

    Optional<Integer> findMaxSequenceByType(
            DeliveryManagerType type
    );

    Optional<Integer> findMaxSequenceByHubIdAndType(
            UUID hubId,
            DeliveryManagerType type
    );

    Optional<DeliveryManager> findById(UUID managerId);

    Page<DeliveryManager> findAll(Pageable pageable);
}