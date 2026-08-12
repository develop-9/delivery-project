package com.delivery_project.delivery_service.delivery.presentation.api_controller;

import com.delivery_project.delivery_service.delivery.application.command.DeliveryDeleteCommand;
import com.delivery_project.delivery_service.delivery.application.command_service.DeliveryCommandService;
import com.delivery_project.delivery_service.delivery.application.query.DeliveryGetQuery;
import com.delivery_project.delivery_service.delivery.application.query.DeliveryListQuery;
import com.delivery_project.delivery_service.delivery.application.query.DeliveryRoutesGetQuery;
import com.delivery_project.delivery_service.delivery.application.query_service.DeliveryQueryService;
import com.delivery_project.delivery_service.delivery.application.query_service.DeliveryRouteQueryService;
import com.delivery_project.delivery_service.delivery.application.result.*;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryStatus;
import com.delivery_project.delivery_service.delivery.presentation.request.DeliveryStatusUpdateRequest;
import com.delivery_project.delivery_service.delivery.presentation.request.DeliveryUpdateRequest;
import com.delivery_project.delivery_service.delivery.presentation.response.*;
import com.delivery_project.delivery_service.global.response.PageResponse;
import com.delivery_project.delivery_service.global.response.SuccessResponse;
import com.delivery_project.delivery_service.global.security.JwtPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/deliveries")
public class DeliveryApiController {

    private final DeliveryCommandService deliveryCommandService;
    private final DeliveryQueryService deliveryQueryService;
    private final DeliveryRouteQueryService deliveryRouteQueryService;

    @PatchMapping("/{deliveryId}/status")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<DeliveryStatusUpdateResponse> updateDeliveryStatus(
            @PathVariable UUID deliveryId,
            @Valid @RequestBody DeliveryStatusUpdateRequest request,
            @AuthenticationPrincipal JwtPrincipal principal
    ){
        DeliveryStatusUpdateResult result =
                deliveryCommandService.updateStatus(
                        request.toCommand(
                                deliveryId,
                                principal.userId(),
                                principal.role())
                );
        return SuccessResponse.success(
                DeliveryStatusUpdateResponse.from(result)
        );
    }

    @GetMapping("/{deliveryId}")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<DeliveryDetailResponse> getDelivery(
            @PathVariable UUID deliveryId,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        DeliveryGetQuery query =
                DeliveryGetQuery.from(
                        deliveryId,
                        principal.userId(),
                        principal.role()
                );

        DeliveryDetailResult result =
                deliveryQueryService.getDelivery(query);

        return SuccessResponse.success(
                DeliveryDetailResponse.from(result)
        );
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<PageResponse<DeliveryListResponse>> getDeliveries(
            @RequestParam(required = false) UUID orderId,
            @RequestParam(required = false) DeliveryStatus status,
            @RequestParam(required = false) UUID departureHubId,
            @RequestParam(required = false) UUID destinationHubId,
            @RequestParam(required = false) UUID companyDeliveryManagerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal JwtPrincipal principal
    ){
        DeliveryListQuery query =
                DeliveryListQuery.of(
                        orderId,
                        status,
                        departureHubId,
                        destinationHubId,
                        companyDeliveryManagerId,
                        page,
                        size,
                        sortBy,
                        direction,
                        principal.userId(),
                        principal.role()
                );

        Page<DeliveryListResult> result =
                deliveryQueryService.getDeliveries(query);

        PageResponse<DeliveryListResponse> response =
                PageResponse.from(
                        result,
                        DeliveryListResponse::from
                );

        return SuccessResponse.success(response);
    }

    @GetMapping("/{deliveryId}/delivery-routes")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<List<DeliveryRouteDetailResponse>> getDeliveryRoutes(
            @PathVariable UUID deliveryId,
            @AuthenticationPrincipal JwtPrincipal principal
    ){
        DeliveryRoutesGetQuery query =
                DeliveryRoutesGetQuery.from(
                        deliveryId,
                        principal.userId(),
                        principal.role()
                );

        List<DeliveryRouteDetailResult> results =
                deliveryRouteQueryService
                        .getDeliveryRoutes(query);

        List<DeliveryRouteDetailResponse> response =
                results.stream()
                        .map(DeliveryRouteDetailResponse::from)
                        .toList();

        return SuccessResponse.success(response);
    }

    @PatchMapping("/{deliveryId}")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<DeliveryUpdateResponse> updateDelivery(
            @PathVariable UUID deliveryId,
            @RequestBody DeliveryUpdateRequest request,
            @AuthenticationPrincipal JwtPrincipal principal
    ){
        DeliveryUpdateResult result =
                deliveryCommandService.update(
                        request.toCommand(
                                deliveryId,
                                principal.userId(),
                                principal.role()
                        )
                );

        return SuccessResponse.success(
                DeliveryUpdateResponse.from(result)
        );
    }

    @DeleteMapping("/{deliveryId}")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<DeliveryDeleteResponse> deleteDelivery(
            @PathVariable UUID deliveryId,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        DeliveryDeleteCommand command =
                new DeliveryDeleteCommand(
                        deliveryId,
                        principal.userId(),
                        principal.role()
                );
        DeliveryDeleteResult result =
                deliveryCommandService.delete(command);

        return SuccessResponse.success(
                DeliveryDeleteResponse.from(result)
        );
    }

}
