package com.delivery_project.delivery_service.delivery.presentation.api_controller;

import com.delivery_project.delivery_service.delivery.application.command_service.DeliveryManagerCommandService;
import com.delivery_project.delivery_service.delivery.application.query_service.DeliveryManagerQueryService;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryManagerCreateResult;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryManagerDetailResult;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryManagerListResult;
import com.delivery_project.delivery_service.delivery.presentation.request.DeliveryManagerCreateRequest;
import com.delivery_project.delivery_service.delivery.presentation.response.DeliveryManagerCreateResponse;
import com.delivery_project.delivery_service.delivery.presentation.response.DeliveryManagerDetailResponse;
import com.delivery_project.delivery_service.delivery.presentation.response.DeliveryManagerListResponse;
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
        DeliveryManagerDetailResult result =
                deliveryManagerQueryService.getDeliveryManager(managerId);

        return SuccessResponse.success(DeliveryManagerDetailResponse.from(result));
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public SuccessResponse<PageResponse<DeliveryManagerListResponse>>
    getDeliveryManagers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ){
        Page<DeliveryManagerListResult> result =
                deliveryManagerQueryService.getDeliveryManagers(
                        page, size
                );

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
            @RequestHeader("X-User-Id") UUID userId // Todo: api gateway 형식에 맞게 추후 수정 필요
    ){
        DeliveryManagerDetailResult result =
                deliveryManagerQueryService.getMyDeliveryManager(userId);

        return SuccessResponse.success(
                DeliveryManagerDetailResponse.from(result));
    }
}
