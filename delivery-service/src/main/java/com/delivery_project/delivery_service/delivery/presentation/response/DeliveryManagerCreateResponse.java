package com.delivery_project.delivery_service.delivery.presentation.response;

import com.delivery_project.delivery_service.delivery.application.result.DeliveryManagerCreateResult;

import java.util.UUID;

public record DeliveryManagerCreateResponse(
        UUID managerId
) {
    public static DeliveryManagerCreateResponse from(
            DeliveryManagerCreateResult result
    ){
        return new DeliveryManagerCreateResponse(
                result.managerId()
        );
    }
}
