package com.delivery_project.delivery_service.delivery.presentation.api_controller;

import com.delivery_project.delivery_service.delivery.application.command.DeliveryManagerDeleteCommand;
import com.delivery_project.delivery_service.delivery.application.command.DeliveryManagerGetCommand;
import com.delivery_project.delivery_service.delivery.application.command.DeliveryManagerGetMyCommand;
import com.delivery_project.delivery_service.delivery.application.command.DeliveryManagerListCommand;
import com.delivery_project.delivery_service.delivery.application.command_service.DeliveryManagerCommandService;
import com.delivery_project.delivery_service.delivery.application.query_service.DeliveryManagerQueryService;
import com.delivery_project.delivery_service.delivery.application.result.*;
import com.delivery_project.delivery_service.delivery.presentation.request.DeliveryManagerCreateRequest;
import com.delivery_project.delivery_service.delivery.presentation.request.DeliveryManagerUpdateRequest;
import com.delivery_project.delivery_service.delivery.presentation.response.*;
import com.delivery_project.delivery_service.global.response.PageResponse;
import com.delivery_project.delivery_service.global.response.SuccessResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/delivery-managers")
public class DeliveryManagerApiController {
    private final DeliveryManagerCommandService deliveryManagerCommandService;
    private final DeliveryManagerQueryService deliveryManagerQueryService;

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

    @GetMapping("/{managerId}")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<DeliveryManagerDetailResponse> getDeliveryManager(
            @PathVariable UUID managerId
    ){
        DeliveryManagerGetCommand command =
                DeliveryManagerGetCommand.from(managerId);

        DeliveryManagerDetailResult result =
                deliveryManagerQueryService.getDeliveryManager(command);

        return SuccessResponse.success(DeliveryManagerDetailResponse.from(result));
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<PageResponse<DeliveryManagerListResponse>>
    getDeliveryManagers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        DeliveryManagerListCommand command =
                DeliveryManagerListCommand.of(
                        page,
                        size
                );

        Page<DeliveryManagerListResult> result =
                deliveryManagerQueryService.getDeliveryManagers(command);

        PageResponse<DeliveryManagerListResponse> response =
                PageResponse.from(
                        result,
                        DeliveryManagerListResponse::from
                );
        return SuccessResponse.success(response);
    }

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<DeliveryManagerDetailResponse> getMyDeliveryManager(
            @RequestHeader("X-User-Id") UUID userId
            // TODO: Spring Security 적용 후 @AuthenticationPrincipal 사용
    ){
        DeliveryManagerGetMyCommand command =
                DeliveryManagerGetMyCommand.from(userId);

        DeliveryManagerDetailResult result =
                deliveryManagerQueryService.getMyDeliveryManager(command);

        return SuccessResponse.success(
                DeliveryManagerDetailResponse.from(result));
    }

    @PatchMapping("/{managerId}")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<DeliveryManagerUpdateResponse> updateDeliveryManager(
            @PathVariable UUID managerId,
            @Valid @RequestBody DeliveryManagerUpdateRequest request
    ){
        DeliveryManagerUpdateResult result =
                deliveryManagerCommandService.update(
                        request.toCommand(managerId)
                );
        return SuccessResponse.success(DeliveryManagerUpdateResponse.from(result));
    }

    @DeleteMapping("/{managerId}")
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<DeliveryManagerDeleteResponse> deleteDeliveryManager(
            @PathVariable UUID managerId,
            @RequestHeader("X-User-Id") UUID deletedBy
            // TODO: Spring Security 적용 후 @AuthenticationPrincipal 사용
    ){
        DeliveryManagerDeleteCommand command =
                DeliveryManagerDeleteCommand.of(
                        managerId,
                        deletedBy
                );

        DeliveryManagerDeleteResult result =
                deliveryManagerCommandService.delete(command);

        return SuccessResponse.success(
                DeliveryManagerDeleteResponse.from(result)
        );
    }
}
