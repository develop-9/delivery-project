package com.delivery_project.delivery_service.delivery.infrastructure.adapter;

import com.delivery_project.delivery_service.delivery.application.port.HubRoutePort;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryPath;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryPathSegment;
import com.delivery_project.delivery_service.delivery.infrastructure.client.HubInternalClient;
import com.delivery_project.delivery_service.delivery.infrastructure.client.dto.HubRoutePathResponse;
import com.delivery_project.delivery_service.delivery.infrastructure.client.dto.InternalApiErrorResponse;
import com.delivery_project.delivery_service.delivery.infrastructure.client.dto.InternalApiResponse;
import com.delivery_project.delivery_service.global.exception.BusinessException;
import com.delivery_project.delivery_service.global.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HubRouteFeignAdapter implements HubRoutePort {

    private final HubInternalClient hubInternalClient;
    private final ObjectMapper objectMapper;

    @Override
    public DeliveryPath getDeliveryPath(
            UUID departureHubId,
            UUID destinationHubId
    ) {
        try {
            InternalApiResponse<HubRoutePathResponse> response =
                    hubInternalClient.getDeliveryRoutePath(
                            departureHubId,
                            destinationHubId
                    );

            List<DeliveryPathSegment> segments =
                    response.data()
                            .segments()
                            .stream()
                            .map(segment ->
                                    new DeliveryPathSegment(
                                            segment.sequence(),
                                            segment.departureHubId(),
                                            segment.arrivalHubId(),
                                            segment.distanceKm(),
                                            segment.durationMin()
                                    )
                            )
                            .toList();

            return new DeliveryPath(segments);

        } catch (FeignException exception) {
            throw convertHubException(exception);
        }
    }

    private BusinessException convertHubException(
            FeignException exception
    ) {
        if (exception.status() >= 500) {
            return new BusinessException(
                    ErrorCode.HUB_SERVICE_UNAVAILABLE
            );
        }

        String hubErrorCode =
                extractHubErrorCode(exception);

        if ("HUB_NOT_FOUND".equals(hubErrorCode)) {
            return new BusinessException(
                    ErrorCode.HUB_NOT_FOUND
            );
        }

        if ("HUB_ROUTE_PATH_NOT_FOUND".equals(hubErrorCode)
                || "HUB_ROUTE_NOT_FOUND".equals(hubErrorCode)) {
            return new BusinessException(
                    ErrorCode.HUB_ROUTE_NOT_FOUND
            );
        }

        if ("SAME_HUB_NOT_ALLOWED".equals(hubErrorCode)
                || exception.status() == 400) {
            return new BusinessException(
                    ErrorCode.INVALID_REQUEST
            );
        }

        return new BusinessException(
                ErrorCode.HUB_SERVICE_UNAVAILABLE
        );
    }

    private String extractHubErrorCode(
            FeignException exception
    ) {
        try {
            String responseBody =
                    exception.contentUTF8();

            if (responseBody == null
                    || responseBody.isBlank()) {
                return null;
            }

            InternalApiErrorResponse response =
                    objectMapper.readValue(
                            responseBody,
                            InternalApiErrorResponse.class
                    );

            if (response.error() == null) {
                return null;
            }

            return response.error().errorCode();

        } catch (JsonProcessingException e) {
            return null;
        }
    }
}