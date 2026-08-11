package com.delivery_project.delivery_service.delivery.application.result;

import com.delivery_project.delivery_service.delivery.domain.entity.Delivery;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryStatus;

import java.time.Instant;
import java.util.UUID;

public record DeliveryListResult(
        UUID deliveryId,
        UUID orderId,
        UUID departureHubId,
        UUID destinationHubId,
        String deliveryAddress,
        String receiverName,
        DeliveryStatus status,
        UUID companyDeliveryManagerId,
        Instant createdAt
) {
    public static DeliveryListResult from(
            Delivery delivery
    ){
        return new DeliveryListResult(
                delivery.getId(),
                delivery.getOrderId(),
                delivery.getDepartureHubId(),
                delivery.getDestinationHubId(),
                delivery.getDeliveryAddress(),
                delivery.getReceiverName(),
                delivery.getStatus(),
                delivery.getCompanyDeliveryManagerId(),
                delivery.getCreatedAt()
        );
    }
}
