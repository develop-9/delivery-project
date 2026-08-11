package com.delivery_project.delivery_service.delivery.application.command_service;

import com.delivery_project.delivery_service.delivery.application.command.DeliveryRouteStatusUpdateCommand;
import com.delivery_project.delivery_service.delivery.application.port.UserPort;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryRouteStatusUpdateResult;
import com.delivery_project.delivery_service.delivery.application.result.UserAuthorizationInfo;
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
import com.delivery_project.delivery_service.global.security.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryRouteCommandService {

    private final DeliveryRouteCommandRepository deliveryRouteCommandRepository;
    private final DeliveryManagerCommandRepository deliveryManagerCommandRepository;
    private final DeliveryManagerSequenceCommandRepository deliveryManagerSequenceCommandRepository;
    private final DeliveryCommandRepository deliveryCommandRepository;
    private final UserPort userPort;

    // =========================
    // 외부 진입점
    // =========================
    @Transactional
    public DeliveryRouteStatusUpdateResult updateStatus(
            DeliveryRouteStatusUpdateCommand command
    ) {
        DeliveryRoute route =
                deliveryRouteCommandRepository
                        .findByIdForUpdate(command.routeId())
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.DELIVERY_ROUTE_NOT_FOUND
                                )
                        );

        validatePermission(command, route);

        DeliveryRouteStatus previousStatus =
                route.getStatus();

        if(command.status() == DeliveryRouteStatus.IN_TRANSIT){
            start(route);
        }else if(command.status() == DeliveryRouteStatus.ARRIVED){
            validateArriveRequest(command);

            arrive(
                    route,
                    command.actualDistanceKm(),
                    command.actualDurationMin()
            );
        }else{
            throw new BusinessException(
                    ErrorCode.INVALID_ROUTE_STATUS_TRANSITION
            );
        }

        return new DeliveryRouteStatusUpdateResult(
                route.getId(),
                previousStatus,
                route.getStatus(),
                route.getUpdatedAt()
        );
    }
    // =========================
    // 상태 변경
    // =========================
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
    private void start(
            DeliveryRoute route
    ) {
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

    private void arrive(
            DeliveryRoute route,
            BigDecimal actualDistanceKm,
            Integer actualDurationMin
    ) {
        route.arrive(
                actualDistanceKm,
                actualDurationMin
        );
        // 2. 허브 배송 담당자 반환
        UUID managerId = route.getDeliveryManagerId();

        if(managerId == null){
            throw new BusinessException(
                    ErrorCode.DELIVERY_MANAGER_NOT_FOUND
            );
        }

        DeliveryManager manager =
                deliveryManagerCommandRepository
                        .findById(managerId)
                        .orElseThrow(()->
                                new BusinessException(
                                        ErrorCode.DELIVERY_MANAGER_NOT_FOUND
                                )
                        );
        manager.releaseFromDelivery();

        // 3. 마지막 Route인지 확인
        DeliveryRoute lastRoute =
            deliveryRouteCommandRepository
                    .findLastByDeliveryId(route.getDeliveryId())
                            .orElseThrow(()->
                                    new BusinessException(
                                            ErrorCode.DELIVERY_ROUTE_NOT_FOUND
                                    )
                            );

        // 4. 마지막 Route라면 전체 Delivery도 목적지 허브 도착 처리
        if(route.getId().equals(lastRoute.getId())){
            Delivery delivery =
                    deliveryCommandRepository
                            .findById(route.getDeliveryId())
                            .orElseThrow(()->
                                    new BusinessException(
                                            ErrorCode.DELIVERY_NOT_FOUND
                                    )
                            );
            // 마지막 허브 도착
            delivery.arriveAtDestinationHub();

            // COMPANY_DELIVERY 배정
            DeliveryManagerSequence companySequence =
                    deliveryManagerSequenceCommandRepository
                            .findForUpdate(
                                    DeliveryManagerType.COMPANY_DELIVERY,
                                    delivery.getDestinationHubId()
                            )
                            .orElseThrow(()->
                                    new BusinessException(
                                            ErrorCode.COMPANY_DELIVERY_MANAGER_NOT_AVAILABLE
                                    )
                            );
            DeliveryManager companyManager =
                    findNextCompanyDeliveryManager(
                            delivery.getDestinationHubId(),
                            companySequence
                    );

            companyManager.assignToDelivery();

            delivery.assignCompanyDeliveryManager(
                    companyManager.getId()
            );

            companySequence.updateLastAssignedSequence(
                    companyManager.getDeliverySequence()
            );

            deliveryManagerCommandRepository.save(companyManager);
            deliveryManagerSequenceCommandRepository.save(companySequence);
            deliveryCommandRepository.save(delivery);
        }

        deliveryRouteCommandRepository.save(route);
        deliveryManagerCommandRepository.save(manager);
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

    private DeliveryManager findNextCompanyDeliveryManager(
            UUID hubId,
            DeliveryManagerSequence sequence
    ){
        return deliveryManagerCommandRepository
                .findNextAvailableCompanyManager(
                        hubId,
                        sequence.getLastAssignedSequence()
                )
                .orElseGet(()->
                        deliveryManagerCommandRepository
                                .findFirstAvailableCompanyManager(hubId)
                                .orElseThrow(()->
                                        new BusinessException(
                                                ErrorCode.COMPANY_DELIVERY_MANAGER_NOT_AVAILABLE
                                        )
                                )
                );
    }

    private void validateArriveRequest(
            DeliveryRouteStatusUpdateCommand command
    ){
        if (command.actualDistanceKm() == null
                || command.actualDurationMin() == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST
            );
        }
    }

    private void validatePermission(
            DeliveryRouteStatusUpdateCommand command,
            DeliveryRoute route
    ) {
        if (command.requesterRole() == Role.MASTER) {
            return;
        }

        if (command.requesterRole() == Role.COMPANY_MANAGER) {
            throw new BusinessException(
                    ErrorCode.UPDATE_DELIVERY_ROUTE_FORBIDDEN
            );
        }

        if (command.requesterRole() == Role.HUB_MANAGER) {
            validateHubManagerRoutePermission(
                    command.requesterId(),
                    route
            );
            return;
        }

        if (command.requesterRole() == Role.DELIVERY_MANAGER) {

            // 배송 담당자는 Route 시작 불가
            if (command.status() == DeliveryRouteStatus.IN_TRANSIT) {
                throw new BusinessException(
                        ErrorCode.UPDATE_DELIVERY_ROUTE_FORBIDDEN
                );
            }

            // ARRIVED 처리만 본인 담당 Route인지 검증
            DeliveryManager manager =
                    deliveryManagerCommandRepository
                            .findByUserId(command.requesterId())
                            .orElseThrow(() ->
                                    new BusinessException(
                                            ErrorCode.UPDATE_DELIVERY_ROUTE_FORBIDDEN
                                    )
                            );

            if (!manager.getId().equals(
                    route.getDeliveryManagerId()
            )) {
                throw new BusinessException(
                        ErrorCode.UPDATE_DELIVERY_ROUTE_FORBIDDEN
                );
            }

            return;
        }

        throw new BusinessException(
                ErrorCode.UPDATE_DELIVERY_ROUTE_FORBIDDEN
        );
    }

    private void validateHubManagerRoutePermission(
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
                    ErrorCode.UPDATE_DELIVERY_ROUTE_FORBIDDEN
            );
        }

        boolean relatedHub =
                requesterHubId.equals(route.getDepartureHubId())
                        || requesterHubId.equals(route.getArrivalHubId()
                );

        if (!relatedHub) {
            throw new BusinessException(
                    ErrorCode.UPDATE_DELIVERY_ROUTE_FORBIDDEN
            );
        }
    }
}
