package com.delivery_project.delivery_service.delivery.application.command_service;

import com.delivery_project.delivery_service.delivery.application.command.DeliveryCancelCommand;
import com.delivery_project.delivery_service.delivery.application.command.DeliveryCreateCommand;
import com.delivery_project.delivery_service.delivery.application.port.HubRoutePort;
import com.delivery_project.delivery_service.delivery.application.port.UserPort;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryCancelResult;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryCreateResult;
import com.delivery_project.delivery_service.delivery.application.result.DeliveryPath;
import com.delivery_project.delivery_service.delivery.application.result.ReceiverInfo;
import com.delivery_project.delivery_service.delivery.domain.entity.Delivery;
import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryManager;
import com.delivery_project.delivery_service.delivery.domain.entity.DeliveryRoute;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryRouteStatus;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryCommandRepository;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryManagerCommandRepository;
import com.delivery_project.delivery_service.delivery.domain.repository.DeliveryRouteCommandRepository;
import com.delivery_project.delivery_service.global.exception.BusinessException;
import com.delivery_project.delivery_service.global.exception.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryCommandService {

    private final DeliveryCommandRepository deliveryCommandRepository;
    private final DeliveryRouteCommandRepository deliveryRouteCommandRepository;
    private final DeliveryManagerCommandRepository deliveryManagerCommandRepository;

    // Application 계층은 FeignClient를 직접 사용하지 않고 Port에만 의존한다.
    private final UserPort userPort;
    private final HubRoutePort hubRoutePort;

    // 이후 DeliveryManagerPort도 추가 예정 =
    // 취소 시 배송 담당자 상태 복원을 위해 DeliveryManager 관련 의존성이 추가될 예정

    public DeliveryCreateResult create(
            DeliveryCreateCommand command,
            String authorization
    ){
        // 동일 주문으로 이미 생성된 배송이 있는지 먼저 검증한다.
        validateDuplicateOrder(command.orderId());

        /*
         * receiverUserId를 이용해 User Service에서
         * 수령인 이름과 Slack ID를 조회한다.
         *
         * 실제 Feign 호출과 응답 변환은 UserPort 구현체인
         * UserFeignAdapter가 담당한다.
         */
        ReceiverInfo receiver = userPort.getReceiver(
                authorization,
                command.receiverUserId()
        );

        /*
         * Hub Service에서 출발 허브부터 목적지 허브까지의
         * 확정 배송 경로를 조회한다.
         *
         * DeliveryCommandService는 Feign 응답 DTO가 아닌
         * Application 전용 DeliveryPath만 전달받는다.
         */
        DeliveryPath deliveryPath = hubRoutePort.getDeliveryPath(
                authorization,
                command.departureHubId(),
                command.destinationHubId()
        );

        Delivery delivery = Delivery.create(
                command.orderId(),
                command.departureHubId(),
                command.destinationHubId(),
                command.deliveryAddress(),
                receiver.name(),
                receiver.slackId()
        );
        /*
         * DeliveryRoute의 deliveryId에 사용할 UUID를 확보하기 위해
         * Delivery를 먼저 저장한다.
         */
        Delivery savedDelivery =
                deliveryCommandRepository.save(delivery);
        /*
         * Hub Service에서 받은 각 경로 구간을
         * DeliveryRoute Entity로 변환한다.
         *
         * 경로 구간 한 개당 DeliveryRoute 한 건이 생성된다.
         */
        List<DeliveryRoute> deliveryRoutes =
                deliveryPath.segments()
                        .stream()
                        .map(segment -> DeliveryRoute.create(
                                savedDelivery.getId(),
                                segment.sequence(),
                                segment.departureHubId(),
                                segment.arrivalHubId(),
                                segment.distanceKm(),
                                segment.durationMin()
                        ))
                        .toList();
        // 변환된 전체 배송 경로를 한 번에 저장한다.
        deliveryRouteCommandRepository.saveAll(deliveryRoutes);

        return new DeliveryCreateResult(
                savedDelivery.getId(),
                savedDelivery.getOrderId(),
                savedDelivery.getStatus(),
                deliveryRoutes.size()
        );
    }

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
}
