package com.delivery_project.delivery_service.delivery.domain.enums;

public enum DeliveryStatus {

    PENDING,
    HUB_MOVING,
    HUB_ARRIVED,
    DELIVERING,
    COMPLETED,
    CANCELED;

    public boolean isCancelable(){
        return this == PENDING
                || this == HUB_MOVING
                || this == HUB_ARRIVED
                || this == DELIVERING;
    }
}
