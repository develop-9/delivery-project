package com.delivery_project.delivery_service.delivery.presentation.internal_controller;

import com.delivery_project.delivery_service.delivery.application.command.DeliveryManagerInternalDeleteCommand;
import com.delivery_project.delivery_service.delivery.application.command_service.DeliveryManagerCommandService;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryManagerInternalDeleteResult;
import com.delivery_project.delivery_service.delivery.presentation.response.DeliveryManagerInternalDeleteResponse;
import com.delivery_project.delivery_service.global.response.SuccessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/delivery-managers")
public class DeliveryManagerInternalController {

    private final DeliveryManagerCommandService deliveryManagerCommandService;

    @DeleteMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<DeliveryManagerInternalDeleteResponse>
    deleteDeliveryManagerByUserId(
            @PathVariable UUID userId
            // TODO: User Service 내부 인증으로 변경
    ) {
        DeliveryManagerInternalDeleteCommand command =
                DeliveryManagerInternalDeleteCommand.from(
                        userId
                );

        DeliveryManagerInternalDeleteResult result =
                deliveryManagerCommandService.deleteByUserId(command);

        return SuccessResponse.success(
                DeliveryManagerInternalDeleteResponse.from(result)
        );
    }
}