package com.delivery_project.delivery_service.delivery.infrastructure.persistence;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerStatus;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataDeliveryManagerRepository
        extends JpaRepository<DeliveryManager, UUID> {

    boolean existsByUserId(UUID userId);

    Page<DeliveryManager> findAllByDeletedAtIsNull(
            Pageable pageable
    );
    Optional<DeliveryManager> findByIdAndDeletedAtIsNull(UUID managerId);

    Optional<DeliveryManager> findByUserIdAndDeletedAtIsNull(UUID userId);

    Optional<DeliveryManager>
    findFirstByTypeAndStatusAndDeliverySequenceGreaterThanAndDeletedAtIsNullOrderByDeliverySequenceAsc(
            DeliveryManagerType type,
            DeliveryManagerStatus status,
            Integer lastAssignedSequence
    );

    Optional<DeliveryManager>
    findFirstByTypeAndStatusAndDeletedAtIsNullOrderByDeliverySequenceAsc(
            DeliveryManagerType type,
            DeliveryManagerStatus status
    );

    Optional<DeliveryManager>
    findFirstByHubIdAndTypeAndStatusAndDeliverySequenceGreaterThanAndDeletedAtIsNullOrderByDeliverySequenceAsc(
            UUID hubId,
            DeliveryManagerType type,
            DeliveryManagerStatus status,
            Integer lastAssignedSequence
    );

    Optional<DeliveryManager>
    findFirstByHubIdAndTypeAndStatusAndDeletedAtIsNullOrderByDeliverySequenceAsc(
            UUID hubId,
            DeliveryManagerType type,
            DeliveryManagerStatus status
    );
}
