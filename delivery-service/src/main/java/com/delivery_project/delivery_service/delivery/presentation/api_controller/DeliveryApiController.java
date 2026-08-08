package com.delivery_project.delivery_service.delivery.presentation.api_controller;

import com.delivery_project.delivery_service.delivery.application.command_service.DeliveryCommandService;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryStatusUpdateResult;
import com.delivery_project.delivery_service.delivery.presentation.request.DeliveryStatusUpdateRequest;
import com.delivery_project.delivery_service.delivery.presentation.response.DeliveryStatusUpdateResponse;
import com.delivery_project.delivery_service.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/deliveries")
public class DeliveryApiController {

    private final DeliveryCommandService deliveryCommandService;

    @PatchMapping("/{deliveryId}/status")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<DeliveryStatusUpdateResponse> updateDeliveryStatus(
            @PathVariable UUID deliveryId,
            @Valid @RequestBody DeliveryStatusUpdateRequest request
    ){
        DeliveryStatusUpdateResult result =
                deliveryCommandService.updateStatus(
                        request.toCommand(deliveryId)
                );
        return SuccessResponse.success(
                DeliveryStatusUpdateResponse.from(result)
        );
    }

}
