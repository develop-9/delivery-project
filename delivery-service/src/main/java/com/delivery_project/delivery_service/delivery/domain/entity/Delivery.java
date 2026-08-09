package com.delivery_project.delivery_service.delivery.domain.entity;

import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryStatus;
import com.delivery_project.delivery_service.global.common.BaseDeletableEntity;
import com.delivery_project.delivery_service.global.exception.BusinessException;
import com.delivery_project.delivery_service.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "p_deliveries")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Delivery extends BaseDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /*
     * 테이블 명세에 UNIQUE 제약이 없으므로 Entity에서도 적용하지 않는다.
     * 동일 주문의 배송 중복 생성 검증은 CommandService에서 처리한다.
     */
    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "departure_hub_id", nullable = false)
    private UUID departureHubId; // 출발 허브

    @Column(name = "destination_hub_id", nullable = false)
    private UUID destinationHubId; // 목적지 허브

    @Column(name = "delivery_address", nullable = false, length = 255)
    private String deliveryAddress; // 최종 배송 주소

    @Column(name = "receiver_name", nullable = false, length = 100)
    private String receiverName; // 수령인 이름

    @Column(name = "receiver_slack_id", nullable = false, length = 100)
    private String receiverSlackId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DeliveryStatus status;

    @Column(name = "company_delivery_manager_id")
    private UUID companyDeliveryManagerId;

    private Delivery(
            UUID orderId,
            UUID departureHubId,
            UUID destinationHubId,
            String deliveryAddress,
            String receiverName,
            String receiverSlackId
    ){
        this.orderId = orderId;
        this.departureHubId = departureHubId;
        this.destinationHubId = destinationHubId;
        this.deliveryAddress = deliveryAddress;
        this.receiverName = receiverName;
        this.receiverSlackId = receiverSlackId;
        this.status = DeliveryStatus.PENDING;
        this.companyDeliveryManagerId = null;
    }

    public static Delivery create(
            UUID orderId,
            UUID departureHubId,
            UUID destinationHubId,
            String deliveryAddress,
            String receiverName,
            String receiverSlackId
    ){
        return new Delivery(
                orderId,
                departureHubId,
                destinationHubId,
                deliveryAddress,
                receiverName,
                receiverSlackId
        );
    }

    public void cancel() {
        if (this.status == DeliveryStatus.CANCELED) {
            throw new BusinessException(
                    ErrorCode.DELIVERY_ALREADY_CANCELED
            );
        }

        if (!this.status.isCancelable()) {
            throw new BusinessException(
                    ErrorCode.DELIVERY_CANCEL_NOT_ALLOWED
            );
        }

        this.status = DeliveryStatus.CANCELED;
    }

    public void startHubMoving(){
        if(this.status != DeliveryStatus.PENDING){
            throw new BusinessException(
                    ErrorCode.INVALID_DELIVERY_STATUS_TRANSITION
            );
        }
        this.status = DeliveryStatus.HUB_MOVING;
    }

    public void arriveAtDestinationHub(){
        if(this.status != DeliveryStatus.HUB_MOVING){
            throw new BusinessException(
                    ErrorCode.INVALID_DELIVERY_STATUS_TRANSITION
            );
        }
        this.status = DeliveryStatus.HUB_ARRIVED;
    }

    public void assignCompanyDeliveryManager(
            UUID managerId
    ) {
        if (this.status != DeliveryStatus.HUB_ARRIVED) {
            throw new BusinessException(
                    ErrorCode.INVALID_DELIVERY_STATUS_TRANSITION
            );
        }

        this.companyDeliveryManagerId = managerId;
    }

    public void startCompanyDelivery(){
        if(this.status != DeliveryStatus.HUB_ARRIVED){
            throw new BusinessException(
                    ErrorCode.INVALID_DELIVERY_STATUS_TRANSITION
            );
        }

        if(this.companyDeliveryManagerId == null){
            throw new BusinessException(
                    ErrorCode.COMPANY_DELIVERY_MANAGER_NOT_ASSIGNED
            );
        }

        this.status = DeliveryStatus.DELIVERING;
    }

    public void complete(){
        if(this.status != DeliveryStatus.DELIVERING){
            throw new BusinessException(
                    ErrorCode.INVALID_DELIVERY_STATUS_TRANSITION
            );
        }

        this.status = DeliveryStatus.COMPLETED;
    }

    public void update(
            String deliveryAddress,
            String receiverName,
            String receiverSlackId
    ){
        if(this.status != DeliveryStatus.PENDING){
            throw new BusinessException(
                    ErrorCode.DELIVERY_ALREADY_STARTED
            );
        }

        if(deliveryAddress != null){
            this.deliveryAddress = deliveryAddress;
        }

        if(receiverName != null && receiverSlackId != null){
            this.receiverName = receiverName;
            this.receiverSlackId = receiverSlackId;
        }

    }
}
