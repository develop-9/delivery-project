package com.delivery_project.delivery_service.delivery.application.query_service;

import com.delivery_project.delivery_service.delivery.application.port.UserPort;
import com.delivery_project.delivery_service.delivery.application.query.DeliveryRouteGetQuery;
import com.delivery_project.delivery_service.delivery.application.query.DeliveryRoutesByOrderQuery;
import com.delivery_project.delivery_service.delivery.application.query.DeliveryRoutesGetQuery;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryRouteDetailResult;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryRoutesByOrderResult;
import com.delivery_project.delivery_service.delivery.application.result.UserAuthorizationInfo;
import com.delivery_project.delivery_service.delivery.domain.entity.Delivery;
import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryRoute;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryManagerQueryRepository;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryQueryRepository;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryRouteQueryRepository;
import com.delivery_project.delivery_service.global.exception.BusinessException;
import com.delivery_project.delivery_service.global.exception.ErrorCode;
import com.delivery_project.delivery_service.global.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeliveryRouteQueryService {

    private final DeliveryQueryRepository deliveryQueryRepository;
    private final DeliveryManagerQueryRepository deliveryManagerQueryRepository;
    private final DeliveryRouteQueryRepository deliveryRouteQueryRepository;
    private final UserPort userPort;

    public DeliveryRoutesByOrderResult getRoutesByOrder(
            DeliveryRoutesByOrderQuery query
    ){
        Delivery delivery =
                deliveryQueryRepository
                        .findByOrderId(query.orderId())
                        .orElseThrow(()->
                                new BusinessException(
                                        ErrorCode.DELIVERY_NOT_FOUND
                                )
                        );
        List<DeliveryRoute> routes =
                deliveryRouteQueryRepository
                        .findAllByDeliveryIdOrderBySequenceAsc(
                                delivery.getId()
                        );
        if(routes.isEmpty()){
            throw new BusinessException(
                    ErrorCode.DELIVERY_ROUTE_NOT_FOUND
            );
        }

        return DeliveryRoutesByOrderResult.of(
                delivery,
                routes
        );
    }

    public DeliveryRouteDetailResult getDeliveryRoute(
            DeliveryRouteGetQuery query
    ){
        DeliveryRoute route =
                deliveryRouteQueryRepository
                        .findById(query.routeId())
                        .orElseThrow(()->
                                new BusinessException(
                                        ErrorCode.DELIVERY_ROUTE_NOT_FOUND
                                )
                        );
        validateRouteReadPermission(
                query,
                route
        );

        return DeliveryRouteDetailResult.from(route);
    }

    public List<DeliveryRouteDetailResult> getDeliveryRoutes(
            DeliveryRoutesGetQuery query
    ){
        Delivery delivery =
                deliveryQueryRepository
                        .findById(query.deliveryId())
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.DELIVERY_NOT_FOUND
                                )
                        );

        validateRoutesReadPermission(
                query,
                delivery
        );


        List<DeliveryRoute> routes =
                deliveryRouteQueryRepository
                        .findAllByDeliveryIdOrderBySequenceAsc(
                                delivery.getId()
                        );

        if (routes.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.DELIVERY_ROUTE_NOT_FOUND
            );
        }

        return routes.stream()
                .map(DeliveryRouteDetailResult::from)
                .toList();
    }

    private void validateRouteReadPermission(
            DeliveryRouteGetQuery query,
            DeliveryRoute route
    ) {
        if (query.requesterRole() == Role.MASTER) {
            return;
        }

        if (query.requesterRole() == Role.HUB_MANAGER) {
            validateHubManagerRouteReadPermission(
                    query.requesterId(),
                    route
            );
            return;
        }

        if (query.requesterRole() == Role.COMPANY_MANAGER) {
            // TODO: 담당 업체 기준 검증
            return;
        }

        if (query.requesterRole() == Role.DELIVERY_MANAGER) {
            DeliveryManager manager =
                    deliveryManagerQueryRepository
                            .findByUserId(query.requesterId())
                            .orElseThrow(() ->
                                    new BusinessException(
                                            ErrorCode.READ_DELIVERY_ROUTE_FORBIDDEN
                                    )
                            );

            // 해당 Route 담당자
            if (manager.getId().equals(
                    route.getDeliveryManagerId()
            )) {
                return;
            }

            // 해당 Delivery의 업체 배송 담당자인지도 확인
            Delivery delivery =
                    deliveryQueryRepository
                            .findById(route.getDeliveryId())
                            .orElseThrow(() ->
                                    new BusinessException(
                                            ErrorCode.DELIVERY_NOT_FOUND
                                    )
                            );

            if (manager.getId().equals(
                    delivery.getCompanyDeliveryManagerId()
            )) {
                return;
            }
        }

        throw new BusinessException(
                ErrorCode.READ_DELIVERY_ROUTE_FORBIDDEN
        );
    }

    private void validateRoutesReadPermission(
            DeliveryRoutesGetQuery query,
            Delivery delivery
    ) {
        if (query.requesterRole() == Role.MASTER) {
            return;
        }

        if (query.requesterRole() == Role.HUB_MANAGER) {
            validateHubManagerRoutesReadPermission(
                    query.requesterId(),
                    delivery
            );
            return;
        }

        if (query.requesterRole() == Role.COMPANY_MANAGER) {
            // TODO: 담당 업체 기준 검증
            return;
        }

        if (query.requesterRole() == Role.DELIVERY_MANAGER) {
            DeliveryManager manager =
                    deliveryManagerQueryRepository
                            .findByUserId(query.requesterId())
                            .orElseThrow(() ->
                                    new BusinessException(
                                            ErrorCode.READ_DELIVERY_ROUTE_FORBIDDEN
                                    )
                            );

            // 업체 배송 담당자인 경우
            if (manager.getId().equals(
                    delivery.getCompanyDeliveryManagerId()
            )) {
                return;
            }

            // 허브 배송 담당자인 경우
            boolean assignedRouteExists =
                    deliveryRouteQueryRepository
                            .existsByDeliveryIdAndDeliveryManagerId(
                                    delivery.getId(),
                                    manager.getId()
                            );

            if (assignedRouteExists) {
                return;
            }
        }

        throw new BusinessException(
                ErrorCode.READ_DELIVERY_ROUTE_FORBIDDEN
        );
    }

    private void validateHubManagerRouteReadPermission(
            UUID requesterId,
            DeliveryRoute route
    ) {
        UserAuthorizationInfo requester =
                userPort.getUserAuthorizationInfo(
                        requesterId
                );

        UUID requesterHubId = requester.hubId();

        if (requesterHubId == null) {
            throw new BusinessException(
                    ErrorCode.READ_DELIVERY_ROUTE_FORBIDDEN
            );
        }

        boolean relatedRoute =
                requesterHubId.equals(route.getDepartureHubId())
                        || requesterHubId.equals(route.getArrivalHubId());

        if (!relatedRoute) {
            throw new BusinessException(
                    ErrorCode.READ_DELIVERY_ROUTE_FORBIDDEN
            );
        }
    }

    private void validateHubManagerRoutesReadPermission(
            UUID requesterId,
            Delivery delivery
    ) {
        UserAuthorizationInfo requester =
                userPort.getUserAuthorizationInfo(
                        requesterId
                );

        UUID requesterHubId = requester.hubId();

        if (requesterHubId == null) {
            throw new BusinessException(
                    ErrorCode.READ_DELIVERY_ROUTE_FORBIDDEN
            );
        }

        boolean relatedDelivery =
                deliveryRouteQueryRepository
                        .existsByDeliveryIdAndHubId(
                                delivery.getId(),
                                requesterHubId
                        );

        if (!relatedDelivery) {
            throw new BusinessException(
                    ErrorCode.READ_DELIVERY_ROUTE_FORBIDDEN
            );
        }
    }
}
