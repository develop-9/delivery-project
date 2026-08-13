package com.delivery_project.delivery_service.delivery.infrastructure.persistence;

import com.delivery_project.delivery_service.delivery.domain.entity.Delivery;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataDeliveryRepository
        extends JpaRepository<Delivery, UUID> {

    Optional<Delivery> findByOrderIdAndDeletedAtIsNull(UUID orderId);

    Optional<Delivery> findByIdAndDeletedAtIsNull(UUID deliveryId);

    boolean existsByCompanyDeliveryManagerIdAndStatusInAndDeletedAtIsNull(
            UUID companyDeliveryManagerId,
            Collection<DeliveryStatus> statuses
    );
}
