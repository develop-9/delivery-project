package com.delivery_project.delivery_service.delivery.presentation.api_controller;

import com.delivery_project.delivery_service.delivery.application.command.DeliveryManagerDeleteCommand;
import com.delivery_project.delivery_service.delivery.application.query.DeliveryManagerGetQuery;
import com.delivery_project.delivery_service.delivery.application.query.DeliveryManagerGetMyQuery;
import com.delivery_project.delivery_service.delivery.application.query.DeliveryManagerListQuery;
import com.delivery_project.delivery_service.delivery.application.command_service.DeliveryManagerCommandService;
import com.delivery_project.delivery_service.delivery.application.query_service.DeliveryManagerQueryService;
import com.delivery_project.delivery_service.delivery.application.result.*;
import com.delivery_project.delivery_service.delivery.presentation.request.DeliveryManagerCreateRequest;
import com.delivery_project.delivery_service.delivery.presentation.request.DeliveryManagerUpdateRequest;
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

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/delivery-managers")
public class DeliveryManagerApiController implements DeliveryManagerApi{
    private final DeliveryManagerCommandService deliveryManagerCommandService;
    private final DeliveryManagerQueryService deliveryManagerQueryService;

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SuccessResponse<DeliveryManagerCreateResponse> createDeliveryManager(
            @Valid @RequestBody DeliveryManagerCreateRequest request,
            @AuthenticationPrincipal JwtPrincipal principal
    ){
        DeliveryManagerCreateResult result =
                deliveryManagerCommandService.create(request.toCommand(
                        principal.userId(),
                        principal.role()
                ));

        DeliveryManagerCreateResponse response =
                DeliveryManagerCreateResponse.from(result);

        return SuccessResponse.success(response);
    }

    @Override
    @GetMapping("/{managerId}")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<DeliveryManagerDetailResponse> getDeliveryManager(
            @PathVariable UUID managerId,
            @AuthenticationPrincipal JwtPrincipal principal
    ){
        DeliveryManagerGetQuery query =
                DeliveryManagerGetQuery.from(
                        managerId,
                        principal.userId(),
                        principal.role());

        DeliveryManagerDetailResult result =
                deliveryManagerQueryService.getDeliveryManager(query);

        return SuccessResponse.success(DeliveryManagerDetailResponse.from(result));
    }

    @Override
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<PageResponse<DeliveryManagerListResponse>>
    getDeliveryManagers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal JwtPrincipal principal
    ){
        DeliveryManagerListQuery query =
                DeliveryManagerListQuery.from(
                        page,
                        size,
                        principal.userId(),
                        principal.role()
                );

        Page<DeliveryManagerListResult> result =
                deliveryManagerQueryService.getDeliveryManagers(query);

        PageResponse<DeliveryManagerListResponse> response =
                PageResponse.from(
                        result,
                        DeliveryManagerListResponse::from
                );
        return SuccessResponse.success(response);
    }

    @Override
    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<DeliveryManagerDetailResponse> getMyDeliveryManager(
            @AuthenticationPrincipal JwtPrincipal principal
    ){
        DeliveryManagerGetMyQuery query =
                DeliveryManagerGetMyQuery.from(
                        principal.userId(),
                        principal.role()
                        );

        DeliveryManagerDetailResult result =
                deliveryManagerQueryService.getMyDeliveryManager(query);

        return SuccessResponse.success(
                DeliveryManagerDetailResponse.from(result));
    }

    @Override
    @PatchMapping("/{managerId}")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<DeliveryManagerUpdateResponse> updateDeliveryManager(
            @PathVariable UUID managerId,
            @Valid @RequestBody DeliveryManagerUpdateRequest request,
            @AuthenticationPrincipal JwtPrincipal principal
    ){
        DeliveryManagerUpdateResult result =
                deliveryManagerCommandService.update(
                        request.toCommand(
                                managerId,
                                principal.userId(),
                                principal.role())
                );
        return SuccessResponse.success(DeliveryManagerUpdateResponse.from(result));
    }

    @Override
    @DeleteMapping("/{managerId}")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<DeliveryManagerDeleteResponse> deleteDeliveryManager(
            @PathVariable UUID managerId,
            @AuthenticationPrincipal JwtPrincipal principal
    ){
        DeliveryManagerDeleteCommand command =
                DeliveryManagerDeleteCommand.of(
                        managerId,
                        principal.userId(),
                        principal.role()

                );

        DeliveryManagerDeleteResult result =
                deliveryManagerCommandService.delete(command);

        return SuccessResponse.success(
                DeliveryManagerDeleteResponse.from(result)
        );
    }
}
