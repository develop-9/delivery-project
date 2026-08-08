package com.delivery_project.delivery_service.delivery.presentation.api_controller;

import com.delivery_project.delivery_service.delivery.application.command_service.DeliveryRouteCommandService;
import com.delivery_project.delivery_service.delivery.application.query_service.DeliveryRouteQueryService;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryRouteDetailResult;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryRouteStatusUpdateResult;
import com.delivery_project.delivery_service.delivery.presentation.request.DeliveryRouteStatusUpdateRequest;
import com.delivery_project.delivery_service.delivery.presentation.response.DeliveryRouteDetailResponse;
import com.delivery_project.delivery_service.delivery.presentation.response.DeliveryRouteStatusUpdateResponse;
import com.delivery_project.delivery_service.global.response.SuccessResponse;
import com.delivery_project.delivery_service.global.security.JwtPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/delivery-routes")
public class DeliveryRouteApiController {

    private final DeliveryRouteCommandService deliveryRouteCommandService;
    private final DeliveryRouteQueryService deliveryRouteQueryService;

    @PatchMapping("/{routeId}/status")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<DeliveryRouteStatusUpdateResponse>
    updateDeliveryRouteStatus(
            @PathVariable UUID routeId,
            @Valid @RequestBody DeliveryRouteStatusUpdateRequest request,
            @AuthenticationPrincipal JwtPrincipal principal
    ){
        DeliveryRouteStatusUpdateResult result =
                deliveryRouteCommandService.updateStatus(
                        request.toCommand(
                                routeId,
                                principal.userId(),
                                principal.role()
                        )
                );

        return SuccessResponse.success(
                DeliveryRouteStatusUpdateResponse.from(result)
        );
    }

    @GetMapping("/{routeId}")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<DeliveryRouteDetailResponse> getDeliveryRoute(
            @PathVariable UUID routeId
    ){
        DeliveryRouteDetailResult result =
                deliveryRouteQueryService
                        .getDeliveryRoute(routeId);

        return SuccessResponse.success(
                DeliveryRouteDetailResponse.from(result)
        );
    }

}
