package com.delivery_project.delivery_service.delivery.presentation.internal_controller;

import com.delivery_project.delivery_service.delivery.application.command.DeliveryCreateCommand;
import com.delivery_project.delivery_service.delivery.application.command_service.DeliveryCommandService;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryCreateResult;
import com.delivery_project.delivery_service.delivery.presentation.request.DeliveryCreateRequest;
import com.delivery_project.delivery_service.delivery.presentation.response.DeliveryCreateResponse;
import com.delivery_project.delivery_service.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/v1/deliveries")
public class DeliveryInternalController {

    private final DeliveryCommandService deliveryCommandService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessResponse<DeliveryCreateResponse> createDelivery(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody DeliveryCreateRequest request
    ) {
        DeliveryCreateCommand command =
                request.toCommand();

        DeliveryCreateResult result =
                deliveryCommandService.create(
                        command,
                        authorization
                );

        return SuccessResponse.success(
                DeliveryCreateResponse.from(result)
        );
    }
}
