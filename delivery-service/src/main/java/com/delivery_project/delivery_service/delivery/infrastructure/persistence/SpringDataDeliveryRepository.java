package com.delivery_project.delivery_service.delivery.infrastructure.persistence;

import com.delivery_project.delivery_service.delivery.domain.entity.Delivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataDeliveryRepository
        extends JpaRepository<Delivery, UUID> {

    Optional<Delivery> findByOrderIdAndDeleteAtIsNull(UUID orderId);
}
