package com.delivery_project.delivery_service.delivery.application.query;

public record DeliveryManagerListQuery(
        int page,
        int size
) {

    public static DeliveryManagerListQuery of(
            int page,
            int size
    ) {
        return new DeliveryManagerListQuery(
                page,
                size
        );
    }
}