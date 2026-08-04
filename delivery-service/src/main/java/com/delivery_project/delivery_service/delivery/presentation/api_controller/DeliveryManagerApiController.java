package com.delivery_project.delivery_service.delivery.presentation.api_controller;

import com.delivery_project.delivery_service.delivery.application.command_service.DeliveryManagerCommandService;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryManagerCreateResult;
import com.delivery_project.delivery_service.delivery.presentation.request.DeliveryManagerCreateRequest;
import com.delivery_project.delivery_service.delivery.presentation.response.DeliveryManagerCreateResponse;
import com.delivery_project.delivery_service.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/delivery-managers")
public class DeliveryManagerApiController {
    private final DeliveryManagerCommandService deliveryManagerCommandService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessResponse<DeliveryManagerCreateResponse> createDeliveryManager(
            @Valid @RequestBody DeliveryManagerCreateRequest request
    ){
        DeliveryManagerCreateResult result =
                deliveryManagerCommandService.create(request.toCommand());

        DeliveryManagerCreateResponse response =
                DeliveryManagerCreateResponse.from(result);

        return SuccessResponse.success(response);
    }
}
