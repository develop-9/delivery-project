package com.delivery_project.delivery_service.delivery.domain.repository;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManagerSequence;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryManagerSequenceCommandRepository {
    DeliveryManagerSequence save(
            DeliveryManagerSequence sequence
    );

    Optional<DeliveryManagerSequence> findForUpdate( // 비관적 락
            DeliveryManagerType managerType,
            UUID hubId
    );

    void createIfAbsent(
            DeliveryManagerType managerType,
            UUID hubId
    );
}
