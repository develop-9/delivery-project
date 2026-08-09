package com.delivery_project.delivery_service.delivery.infrastructure.persistence;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryRoute;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryRouteStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataDeliveryRouteRepository
        extends JpaRepository<DeliveryRoute, UUID> {

    Optional<DeliveryRoute> findByIdAndDeletedAtIsNull(UUID routeId);

    List<DeliveryRoute> findAllByDeliveryIdAndStatusAndDeletedAtIsNull(
            UUID deliveryId,
            DeliveryRouteStatus status
    );

    Optional<DeliveryRoute> findByDeliveryIdAndSequenceAndDeletedAtIsNull(
            UUID deliveryId,
            Integer sequence
    );

    Optional<DeliveryRoute> findFirstByDeliveryIdAndDeletedAtIsNullOrderBySequenceDesc(
            UUID deliveryId
    );

    List<DeliveryRoute>
    findAllByDeliveryIdAndDeletedAtIsNullOrderBySequenceAsc(
            UUID deliveryId
    );

    List<DeliveryRoute> findAllByDeliveryIdAndDeletedAtIsNull(
            UUID deliveryId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT dr
        FROM DeliveryRoute dr
        WHERE dr.id = :routeId
          AND dr.deletedAt IS NULL
        """)
    Optional<DeliveryRoute> findByIdForUpdate(
            @Param("routeId") UUID routeId
    );
}
