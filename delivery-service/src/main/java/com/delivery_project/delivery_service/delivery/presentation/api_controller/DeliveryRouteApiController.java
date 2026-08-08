package com.delivery_project.delivery_service.delivery.presentation.api_controller;

import com.delivery_project.delivery_service.delivery.application.command_service.DeliveryRouteCommandService;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryRouteStatusUpdateResult;
import com.delivery_project.delivery_service.delivery.presentation.request.DeliveryRouteStatusUpdateRequest;
import com.delivery_project.delivery_service.delivery.presentation.response.DeliveryRouteStatusUpdateResponse;
import com.delivery_project.delivery_service.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/delivery-routes")
public class DeliveryRouteApiController {

    private final DeliveryRouteCommandService deliveryRouteCommandService;

    @PatchMapping("/{routeId}/status")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<DeliveryRouteStatusUpdateResponse>
    updateDeliveryRouteStatus(
            @PathVariable UUID routeId,
            @Valid @RequestBody DeliveryRouteStatusUpdateRequest request
    ){
        DeliveryRouteStatusUpdateResult result =
                deliveryRouteCommandService.updateStatus(
                        request.toCommand(routeId)
                );

        return SuccessResponse.success(
                DeliveryRouteStatusUpdateResponse.from(result)
        );
    }

}
