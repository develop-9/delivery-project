package com.delivery_project.delivery_service.delivery.presentation.request;

import com.delivery_project.delivery_service.delivery.application.command.DeliveryManagerCreateCommand;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;
import com.delivery_project.delivery_service.global.security.Role;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DeliveryManagerCreateRequest(

        @NotNull(message = "사용자 ID는 필수입니다.")
        UUID userId,

        UUID hubId,

        @NotNull(message = "배송 담당자 타입은 필수입니다.")
        DeliveryManagerType type

) {

    public DeliveryManagerCreateCommand toCommand(
            UUID requesterId,
            Role requesterRole
    ) {
        return new DeliveryManagerCreateCommand(
                userId,
                hubId,
                type,
                requesterId,
                requesterRole
        );
    }
}