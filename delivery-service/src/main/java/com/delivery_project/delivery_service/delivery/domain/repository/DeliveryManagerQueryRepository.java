package com.delivery_project.delivery_service.delivery.domain.repository;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryManagerQueryRepository {

    Optional<DeliveryManager> findById(UUID managerId);

    Page<DeliveryManager> findAll(Pageable pageable);

    Page<DeliveryManager> findAllByHubId(
            UUID hubId,
            Pageable pageable
    );

    Optional<DeliveryManager> findByUserId(UUID userId);
}