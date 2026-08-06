package com.delivery_project.delivery_service.delivery.infrastructure.adapter;

import com.delivery_project.delivery_service.delivery.application.port.HubRoutePort;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryPath;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryPathSegment;
import com.delivery_project.delivery_service.delivery.infrastructure.client.HubInternalClient;
import com.delivery_project.delivery_service.delivery.infrastructure.client.dto.HubRoutePathResponse;
import com.delivery_project.delivery_service.delivery.infrastructure.client.dto.InternalApiResponse;
import com.delivery_project.delivery_service.global.exception.BusinessException;
import com.delivery_project.delivery_service.global.exception.ErrorCode;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class HubRouteFeignAdapter implements HubRoutePort {
    private final HubInternalClient hubInternalClient;

    @Override
    public DeliveryPath getDeliveryPath(
            String authorization,
            UUID departureHubId,
            UUID destinationHubId
    ){
        try {
            InternalApiResponse<HubRoutePathResponse> response =
                    hubInternalClient.getDeliveryRoutePath(
                            authorization,
                            departureHubId,
                            destinationHubId
                    );

            List<DeliveryPathSegment> segments =
                    response.data()
                            .segments()
                            .stream()
                            .map(segment -> new DeliveryPathSegment(
                                    segment.sequence(),
                                    segment.departureHubId(),
                                    segment.arrivalHubId(),
                                    segment.distanceKm(),
                                    segment.durationMin()
                            ))
                            .toList();
            return new DeliveryPath(segments);
        } catch (FeignException.NotFound exception) {
            /*
             * HUB_NOT_FOUND와 HUB_ROUTE_PATH_NOT_FOUND를 정확히
             * 구분하려면 Feign 에러 응답 body의 code를 해석해야 한다.
             * 현재는 임시로 경로 조회 실패로 변환한다.
             */
            throw new BusinessException(
                    ErrorCode.HUB_NOT_FOUND
            );
        } catch (FeignException exception) {
            throw new BusinessException(
                    ErrorCode.INTERNAL_SERVER_ERROR
            );
        }
    }
}
