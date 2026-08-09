package com.delivery_project.delivery_service.delivery.application.command_service;

import com.delivery_project.delivery_service.delivery.application.command.*;
import com.delivery_project.delivery_service.delivery.application.port.HubRoutePort;
import com.delivery_project.delivery_service.delivery.application.port.UserPort;
import com.delivery_project.delivery_service.delivery.application.result.*;
import com.delivery_project.delivery_service.delivery.domain.entity.Delivery;
import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryRoute;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerStatus;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryRouteStatus;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryStatus;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryCommandRepository;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryManagerCommandRepository;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryRouteCommandRepository;
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
public class DeliveryCommandService {

    private final DeliveryCommandRepository deliveryCommandRepository;
    private final DeliveryRouteCommandRepository deliveryRouteCommandRepository;
    private final DeliveryManagerCommandRepository deliveryManagerCommandRepository;
    private final DeliveryPersistenceService deliveryPersistenceService;

    // Application 계층은 FeignClient를 직접 사용하지 않고 Port에만 의존한다.
    private final UserPort userPort;
    private final HubRoutePort hubRoutePort;

    public DeliveryCreateResult create(
            DeliveryCreateCommand command
    ) {
        validateDuplicateOrder(command.orderId());

        // 외부 User Service 호출 - 트랜잭션 밖
        ReceiverInfo receiver =
                userPort.getReceiver(
                        command.receiverUserId()
                );

        // 외부 Hub Service 호출 - 트랜잭션 밖
        DeliveryPath deliveryPath =
                hubRoutePort.getDeliveryPath(
                        command.departureHubId(),
                        command.destinationHubId()
                );

        // 실제 DB 저장 시점부터 트랜잭션 시작 => 다른 파일로 분리하여 @Transaction 기능 분리하여 제공
        return deliveryPersistenceService.create(
                command,
                receiver,
                deliveryPath
        );
    }

    @Transactional
    public DeliveryCancelResult cancel(
            DeliveryCancelCommand command
    ){  // 주문 ID에 연결된 삭제되지 않은 배송을 조회한다.
        Delivery delivery = deliveryCommandRepository
                .findByOrderIdAndDeletedAtIsNull(command.orderId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.DELIVERY_NOT_FOUND
                ));

        /*
         * Delivery Entity 내부에서
         * 중복 취소와 취소 가능 상태를 검증한 뒤 CANCELED로 변경한다.
         */
        delivery.cancel();

        releaseCompanyDeliveryManager(delivery);
        releaseHubDeliveryManagers(delivery.getId());

        /*
         * 변경 감지만으로도 UPDATE는 가능하지만,
         * 최신 updatedAt을 응답에 사용하기 위해 명시적으로 저장한다.
         */
        Delivery savedDelivery =
                deliveryCommandRepository.save(delivery);

        return new DeliveryCancelResult(
                savedDelivery.getId(),
                savedDelivery.getOrderId(),
                savedDelivery.getStatus(),
                savedDelivery.getUpdatedAt()
        );
    }

    @Transactional
    public DeliveryStatusUpdateResult updateStatus(
            DeliveryStatusUpdateCommand command
    ){
        Delivery delivery =
                deliveryCommandRepository
                        .findById(command.deliveryId())
                        .orElseThrow(() ->
                                new BusinessException(
                                        ErrorCode.DELIVERY_NOT_FOUND
                                )
                        );

        validateStatusUpdatePermission(command, delivery);

        DeliveryStatus previousStatus =
                delivery.getStatus();

        if (command.status() == DeliveryStatus.DELIVERING) {
            startCompanyDelivery(delivery);

        } else if (command.status() == DeliveryStatus.COMPLETED) {
            completeCompanyDelivery(delivery);

        } else {
            throw new BusinessException(
                    ErrorCode.INVALID_DELIVERY_STATUS_TRANSITION
            );
        }
        Delivery savedDelivery =
                deliveryCommandRepository.save(delivery);

        return new DeliveryStatusUpdateResult(
                savedDelivery.getId(),
                previousStatus,
                savedDelivery.getStatus(),
                savedDelivery.getCompanyDeliveryManagerId(),
                savedDelivery.getUpdatedAt()
        );
    }

    @Transactional
    public DeliveryUpdateResult update(
            DeliveryUpdateCommand command
    ){
        Delivery delivery =
                deliveryCommandRepository
                        .findById(command.deliveryId())
                        .orElseThrow(()->
                                new BusinessException(
                                        ErrorCode.DELIVERY_NOT_FOUND
                                )
                        );

        ReceiverInfo receiver = null;

        if(command.receiverUserId() != null){
            receiver = userPort.getReceiver(
                    command.receiverUserId()
            );
        }

        delivery.update(
                command.deliveryAddress(),
                receiver != null ? receiver.name() : null,
                receiver != null ? receiver.slackId() : null
        );

        Delivery savedDelivery =
                deliveryCommandRepository.save(delivery);

        return DeliveryUpdateResult.from(savedDelivery);
    }

    @Transactional
    public DeliveryDeleteResult delete(
            DeliveryDeleteCommand command
    ){
        Delivery delivery =
                deliveryCommandRepository
                        .findById(command.deliveryId())
                        .orElseThrow(()->
                                new BusinessException(
                                        ErrorCode.DELIVERY_NOT_FOUND
                                )
                        );
        // PENDING / CANCELED 상태만 삭제 가능
        delivery.validateDeletable();

        List<DeliveryRoute> routes =
                deliveryRouteCommandRepository
                        .findAllByDeliveryIdAndDeletedAtIsNull(
                                delivery.getId()
                        );
        // 혹시 남아있는 담당자가 있다면 AVAILABLE 복원
        releaseAssignedManagersForDelete(delivery);

        // Delivery 논리 삭제
        delivery.delete(command.deletedBy());

        // 연결된 Route 전체 논리 삭제
        for(DeliveryRoute route : routes){
            route.delete(command.deletedBy());
        }

        deliveryRouteCommandRepository.saveAll(routes);

        Delivery savedDelivery =
                deliveryCommandRepository.save(delivery);

        return DeliveryDeleteResult.from(savedDelivery);
    }

    private void validateDuplicateOrder(
            java.util.UUID orderId
    ) {
        /*
         * 동일 orderId의 삭제되지 않은 Delivery가 존재하면
         * 중복 배송 생성 요청으로 판단한다.
         */
        boolean alreadyExists = deliveryCommandRepository
                .findByOrderIdAndDeletedAtIsNull(orderId)
                .isPresent();

        if (alreadyExists) {
            throw new BusinessException(
                    ErrorCode.DELIVERY_ALREADY_EXISTS
            );
        }
    }

    private void releaseCompanyDeliveryManager(
            Delivery delivery
    ){
        UUID managerId = delivery.getCompanyDeliveryManagerId();

        if(managerId == null){
            return;
        }

        DeliveryManager deliveryManager =
                deliveryManagerCommandRepository
                        .findById(managerId)
                        .orElseThrow(()-> new BusinessException(
                                ErrorCode.DELIVERY_MANAGER_NOT_FOUND
                        ));

        deliveryManager.releaseFromDelivery();
    }

    private void releaseHubDeliveryManagers(
            UUID deliveryId
    ){
        List<DeliveryRoute> inTransitRoutes =
                deliveryRouteCommandRepository
                        .findAllByDeliveryIdAndStatusAndDeletedAtIsNull(
                                deliveryId,
                                DeliveryRouteStatus.IN_TRANSIT
                        );

        for(DeliveryRoute route : inTransitRoutes){
            UUID managerId = route.getDeliveryManagerId();

            if(managerId == null){
                continue;
            }

            DeliveryManager deliveryManager =
                    deliveryManagerCommandRepository
                            .findById(managerId)
                            .orElseThrow(()-> new BusinessException(
                                    ErrorCode.DELIVERY_MANAGER_NOT_FOUND
                            ));

            deliveryManager.releaseFromDelivery();
        }
    }

    private void startCompanyDelivery(
            Delivery delivery
    ){
        UUID managerId =
                delivery.getCompanyDeliveryManagerId();

        if(managerId == null){
            throw new BusinessException(
                    ErrorCode.COMPANY_DELIVERY_MANAGER_NOT_ASSIGNED
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
        /*
         * 마지막 Route 도착 시 이미
         * AVAILABLE -> DELIVERING 상태로 배정됐어야 한다.
         * 따라서 assignToDelivery() 호출 x
         */
        if(manager.getStatus()
            != DeliveryManagerStatus.DELIVERING){
            throw new BusinessException(
                    ErrorCode.DELIVERY_MANAGER_NOT_DELIVERING
            );
        }

        delivery.startCompanyDelivery();
    }

    private void completeCompanyDelivery(
            Delivery delivery
    ){
        UUID managerId =
                delivery.getCompanyDeliveryManagerId();

        if(managerId == null){
            throw new BusinessException(
                    ErrorCode.COMPANY_DELIVERY_MANAGER_NOT_ASSIGNED
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

        // DELIVERING 상태에서만 COMPLETED 가능
        delivery.complete();

        // 업체 배송 담당자 복원
        manager.releaseFromDelivery();

        deliveryManagerCommandRepository.save(manager); // AVAILABLE
    }

    private void validateStatusUpdatePermission(
            DeliveryStatusUpdateCommand command,
            Delivery delivery
    ) {
        if (command.requesterRole() == Role.MASTER) {
            return;
        }

        if (command.requesterRole() == Role.COMPANY_MANAGER) {
            throw new BusinessException(
                    ErrorCode.UPDATE_DELIVERY_STATUS_FORBIDDEN
            );
        }

        if (command.requesterRole() == Role.HUB_MANAGER) {
            // TODO: #51 완료 후 담당 Hub 기준 권한 검증 추가
            return;
        }

        if (command.requesterRole() == Role.DELIVERY_MANAGER) {
            DeliveryManager manager =
                    deliveryManagerCommandRepository
                            .findByUserId(command.requesterId())
                            .orElseThrow(() ->
                                    new BusinessException(
                                            ErrorCode.UPDATE_DELIVERY_STATUS_FORBIDDEN
                                    )
                            );

            if (!manager.getId().equals(
                    delivery.getCompanyDeliveryManagerId()
            )) {
                throw new BusinessException(
                        ErrorCode.UPDATE_DELIVERY_STATUS_FORBIDDEN
                );
            }

            return;
        }

        throw new BusinessException(
                ErrorCode.UPDATE_DELIVERY_STATUS_FORBIDDEN
        );
    }

    private void releaseAssignedManagersForDelete(
            Delivery delivery
    ) {
        UUID managerId = delivery.getCompanyDeliveryManagerId();

        if (managerId != null) {
            DeliveryManager manager =
                    deliveryManagerCommandRepository
                            .findById(managerId)
                            .orElseThrow(() ->
                                    new BusinessException(
                                            ErrorCode.DELIVERY_MANAGER_NOT_FOUND
                                    )
                            );

            if (manager.getStatus()
                    == DeliveryManagerStatus.DELIVERING) {
                manager.releaseFromDelivery();
            }
        }

        List<DeliveryRoute> inTransitRoutes =
                deliveryRouteCommandRepository
                        .findAllByDeliveryIdAndStatusAndDeletedAtIsNull(
                                delivery.getId(),
                                DeliveryRouteStatus.IN_TRANSIT
                        );

        for (DeliveryRoute route : inTransitRoutes) {
            UUID hubManagerId = route.getDeliveryManagerId();

            if (hubManagerId == null) {
                continue;
            }

            DeliveryManager manager =
                    deliveryManagerCommandRepository
                            .findById(hubManagerId)
                            .orElseThrow(() ->
                                    new BusinessException(
                                            ErrorCode.DELIVERY_MANAGER_NOT_FOUND
                                    )
                            );

            if (manager.getStatus()
                    == DeliveryManagerStatus.DELIVERING) {
                manager.releaseFromDelivery();
            }
        }
    }
}
