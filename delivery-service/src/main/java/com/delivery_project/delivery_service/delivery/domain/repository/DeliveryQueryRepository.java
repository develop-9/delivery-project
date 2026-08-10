package com.delivery_project.delivery_service.delivery.domain.repository;

import com.delivery_project.delivery_service.delivery.application.query.DeliveryListQuery;
import com.delivery_project.delivery_service.delivery.domain.entity.Delivery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryQueryRepository {

    Optional<Delivery> findById(UUID deliveryId);

    Optional<Delivery> findByOrderId(UUID orderId);

    Page<Delivery> search(
            DeliveryListQuery query,
            Pageable pageable,
            UUID requesterManagerId,
            UUID requesterHubId
    );
}
