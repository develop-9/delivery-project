package com.delivery_project.delivery_service.delivery.application.result;

import com.delivery_project.delivery_service.delivery.domain.entity.Delivery;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryStatus;

import java.util.UUID;

public record DeliveryDetailResult(
        UUID deliveryId,
        UUID orderId,
        UUID departureHubId,
        UUID destinationHubId,
        String deliveryAddress,
        String receiverName,
        String receiverSlackId,
        DeliveryStatus status,
        UUID companyDeliveryManagerId
) {
    public static DeliveryDetailResult from(
            Delivery delivery
    ){
        return new DeliveryDetailResult(
                delivery.getId(),
                delivery.getOrderId(),
                delivery.getDepartureHubId(),
                delivery.getDestinationHubId(),
                delivery.getDeliveryAddress(),
                delivery.getReceiverName(),
                delivery.getReceiverSlackId(),
                delivery.getStatus(),
                delivery.getCompanyDeliveryManagerId()
        );
    }
}
