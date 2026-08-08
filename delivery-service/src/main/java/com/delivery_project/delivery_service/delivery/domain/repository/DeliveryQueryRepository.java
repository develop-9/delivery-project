package com.delivery_project.delivery_service.delivery.domain.repository;

import com.delivery_project.delivery_service.delivery.domain.entity.Delivery;

import java.util.Optional;
import java.util.UUID;

public interface DeliveryQueryRepository {

    Optional<Delivery> findByOrderId(UUID orderId);
}
