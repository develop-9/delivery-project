package com.delivery_project.delivery_service.delivery.infrastructure.persistence;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;
import feign.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataDeliveryManagerRepository
        extends JpaRepository<DeliveryManager, UUID> {

    boolean existsByUserId(UUID userId);

    @Query("""
        SELECT MAX(dm.deliverySequence)
        FROM DeliveryManager dm
        WHERE dm.type = :type
          AND dm.deletedAt IS NULL
        """)
    Optional<Integer> findMaxSequenceByType(
            @Param("type") DeliveryManagerType type
    );

    @Query("""
        SELECT MAX(dm.deliverySequence)
        FROM DeliveryManager dm
        WHERE dm.hubId = :hubId
          AND dm.type = :type
          AND dm.deletedAt IS NULL
        """)
    Optional<Integer> findMaxSequenceByHubIdAndType(
            @Param("hubId") UUID hubId,
            @Param("type") DeliveryManagerType type
    );

    Page<DeliveryManager> findAllByDeletedAtIsNull(
            Pageable pageable
    );
}
