package com.delivery_project.delivery_service.delivery.presentation.api_controller;

import com.delivery_project.delivery_service.delivery.application.command_service.DeliveryRouteCommandService;
import com.delivery_project.delivery_service.delivery.application.query.DeliveryRouteGetQuery;
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
public class DeliveryRouteApiController implements DeliveryRouteApi {

    private final DeliveryRouteCommandService deliveryRouteCommandService;
    private final DeliveryRouteQueryService deliveryRouteQueryService;

    @Override
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

    @Override
    @GetMapping("/{routeId}")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<DeliveryRouteDetailResponse> getDeliveryRoute(
            @PathVariable UUID routeId,
            @AuthenticationPrincipal JwtPrincipal principal
    ){
        DeliveryRouteGetQuery query =
                DeliveryRouteGetQuery.from(
                        routeId,
                        principal.userId(),
                        principal.role()
                );

        DeliveryRouteDetailResult result =
                deliveryRouteQueryService
                        .getDeliveryRoute(query);

        return SuccessResponse.success(
                DeliveryRouteDetailResponse.from(result)
        );
    }

}
