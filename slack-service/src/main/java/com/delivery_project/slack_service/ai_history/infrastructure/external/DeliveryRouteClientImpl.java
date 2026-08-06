package com.delivery_project.slack_service.ai_history.infrastructure.external;

import com.delivery_project.slack_service.ai_history.application.port.DeliveryRouteClient;
import com.delivery_project.slack_service.ai_history.application.result.DeliveryRouteResult;
import com.delivery_project.slack_service.global.exception.BusinessException;
import com.delivery_project.slack_service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DeliveryRouteClientImpl
        implements DeliveryRouteClient {

    private final DeliveryRouteFeignClient deliveryRouteFeignClient;

    @Override
    public DeliveryRouteResult getRoutesByOrderId(
            UUID orderId
    ) {
        DeliveryRouteFeignClient.DeliveryRouteApiResponse response =
                deliveryRouteFeignClient.getRoutesByOrderId(
                        orderId
                );

        if (
                response == null
                        || !response.success()
                        || response.data() == null
        ) {
            throw new BusinessException(
                    ErrorCode.DEPENDENCY_SERVICE_UNAVAILABLE
            );
        }

        DeliveryRouteFeignClient.DeliveryRouteData data =
                response.data();

        List<DeliveryRouteResult.RouteResult> routes =
                data.routes() == null
                        ? List.of()
                        : data.routes()
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                        DeliveryRouteFeignClient.RouteData::sequence
                                )
                        )
                        .map(route ->
                             new DeliveryRouteResult.RouteResult(
                                     route.sequence(),
                                     route.departureHubId(),
                                     route.arrivalHubId(),
                                     route.estimatedDurationMin()
                             )
                        )
                        .toList();

        return new DeliveryRouteResult(
                data.deliveryId(),
                data.orderId(),
                routes
        );
    }
}