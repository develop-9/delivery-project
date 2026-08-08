package com.delivery_project.delivery_service.delivery.application.command_service;

import com.delivery_project.delivery_service.delivery.domain.entity.Delivery;
import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManagerSequence;
import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryRoute;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryRouteStatus;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryCommandRepository;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryManagerCommandRepository;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryManagerSequenceCommandRepository;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryRouteCommandRepository;
import com.delivery_project.delivery_service.global.exception.BusinessException;
import com.delivery_project.delivery_service.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryRouteCommandService {

    private final DeliveryRouteCommandRepository deliveryRouteCommandRepository;
    private final DeliveryManagerCommandRepository deliveryManagerCommandRepository;
    private final DeliveryManagerSequenceCommandRepository deliveryManagerSequenceCommandRepository;
    private final DeliveryCommandRepository deliveryCommandRepository;
/*
Route 조회
→ HUB_DELIVERY 순번 row 비관적 Lock
→ lastAssignedSequence 다음 AVAILABLE 담당자 조회
→ 없으면 처음부터 다시 조회
→ 담당자 AVAILABLE → DELIVERING
→ Route WAITING → IN_TRANSIT
→ lastAssignedSequence 갱신
→ 저장
 */ // Delivery: PENDING -> HUB_MOVING
    // Delivery_Route: WAITING -> IN_TRANSIT
    @Transactional
    public void start(UUID routeId){

        DeliveryRoute route =
                deliveryRouteCommandRepository
                        .findById(routeId)
                        .orElseThrow(()->
                                new BusinessException(
                                        ErrorCode.DELIVERY_ROUTE_NOT_FOUND
                                )
                        );
        validateNoInTransitRoute(route);
        validatePreviousRouteArrived(route);
        /*
         * HUB_DELIVERY 담당자는 전체가 하나의 Round-Robin 그룹이므로
         * hubId = null인 Sequence row를 비관적 락으로 조회한다.
         */
        DeliveryManagerSequence sequence =
                deliveryManagerSequenceCommandRepository
                        .findForUpdate(
                                DeliveryManagerType.HUB_DELIVERY,
                                null
                        )
                        .orElseThrow(()->
                                new BusinessException(
                                        ErrorCode.HUB_DELIVERY_MANAGER_NOT_AVAILABLE
                                )
                        );
        DeliveryManager manager =
                findNextHubDeliveryManager(sequence);

        // 담당자 AVAILABLE -> DELIVERING
        manager.assignToDelivery();

        // Route WAITING -> IN_TRANSIT + 담당자 ID 저장
        route.start(manager.getId());

        // 첫 번째 허브 간 경로가 출발하면 전체 배송(Delivery)도 허브 이동 상태로 변경
        if(route.getSequence() == 1){
            Delivery delivery =
                    deliveryCommandRepository
                            .findById(route.getDeliveryId())
                            .orElseThrow(()->
                                    new BusinessException(
                                            ErrorCode.DELIVERY_NOT_FOUND
                                    )
                            );
            delivery.startHubMoving();

            deliveryCommandRepository.save(delivery);
        }

        // 다음 배정을 위해 마지막 순번 갱신
        sequence.updateLastAssignedSequence(
                manager.getDeliverySequence()
        );

        deliveryManagerCommandRepository.save(manager);
        deliveryRouteCommandRepository.save(route);
        deliveryManagerSequenceCommandRepository.save(sequence);
    }

    private DeliveryManager findNextHubDeliveryManager(
            DeliveryManagerSequence sequence
    ){
        return deliveryManagerCommandRepository
                .findNextAvailableHubManager(
                        sequence.getLastAssignedSequence()
                )
                .orElseGet(()->
                        deliveryManagerCommandRepository
                                .findFirstAvailableHubManager()
                                .orElseThrow(()->
                                        new BusinessException(
                                                ErrorCode.HUB_DELIVERY_MANAGER_NOT_AVAILABLE
                                        )
                        )
                );
    }

    private void validateNoInTransitRoute(DeliveryRoute route){ // 이미 inTransit 상태가 있는지 검증
        List<DeliveryRoute> inTransitRoutes =
                deliveryRouteCommandRepository
                        .findAllByDeliveryIdAndStatusAndDeletedAtIsNull(
                                route.getDeliveryId(),
                                DeliveryRouteStatus.IN_TRANSIT
                        );
        if(!inTransitRoutes.isEmpty()){
            throw new BusinessException(
                    ErrorCode.DELIVERY_ROUTE_ALREADY_IN_TRANSIT
            );
        }
    }

    // 즉 이전 경로가 ARRIVED 상태가 아니라면 다음 경로로 출발시키지 않음
    private void validatePreviousRouteArrived(DeliveryRoute route){ // 이전 Route ARRIVED 확인
        // 첫 번째 Route는 이전 경로가 없으므로 검증하지 않는다.
        if(route.getSequence() == 1){
            return;
        }

        DeliveryRoute previousRoute =
            deliveryRouteCommandRepository
                    .findByDeliveryIdAndSequenceAndDeletedAtIsNull(
                            route.getDeliveryId(),
                            route.getSequence() - 1
                    )
                    .orElseThrow(()->
                            new BusinessException(
                                    ErrorCode.DELIVERY_ROUTE_NOT_FOUND
                            )
                    );
        if(previousRoute.getStatus()
            != DeliveryRouteStatus.ARRIVED){
            throw new BusinessException(
                    ErrorCode.PREVIOUS_ROUTE_NOT_ARRIVED
            );
        }
    }
}
