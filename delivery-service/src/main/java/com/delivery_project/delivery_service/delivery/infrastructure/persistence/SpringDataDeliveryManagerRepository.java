package com.delivery_project.delivery_service.delivery.infrastructure.persistence;

import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataDeliveryManagerRepository
        extends JpaRepository<DeliveryManager, UUID> {

    boolean existsByUserId(UUID userId);
}
