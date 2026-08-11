package com.delivery_project.delivery_service.delivery.domain.entity;

import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerStatus;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;
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
@Table(name = "p_delivery_managers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryManager extends BaseDeletableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "hub_id")
    private UUID hubId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private DeliveryManagerType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DeliveryManagerStatus status;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "delivery_sequence", nullable = false)
    private Integer deliverySequence;

    private DeliveryManager(
            UUID userId,
            UUID hubId,
            DeliveryManagerType type,
            Integer deliverySequence
    ){
        this.userId = userId;
        this.hubId = hubId;
        this.type = type;
        this.status = DeliveryManagerStatus.AVAILABLE;
        this.active = true;
        this.deliverySequence = deliverySequence;
    }

    public static DeliveryManager create(
            UUID userId,
            UUID hubId,
            DeliveryManagerType type,
            Integer deliverySequence
    ){
        validateHubId(type, hubId);

        return new DeliveryManager(
                userId,
                hubId,
                type,
                deliverySequence);
    }

    public void update(
            UUID hubId,
            DeliveryManagerType type
    ){
        validateHubId(type, hubId);
        validateNotDelivering();

        this.hubId = hubId;
        this.type = type;
    }

    public void updateDeliverySequence(
            Integer deliverySequence
    ){
        this.deliverySequence = deliverySequence;
    }

    public void deleteManager(UUID deletedBy) {
        if (this.status == DeliveryManagerStatus.DELIVERING) {
            throw new BusinessException(
                    ErrorCode.DELIVERY_MANAGER_IS_DELIVERING
            );
        }

        super.delete(deletedBy);
    }

    public void releaseFromDelivery() {
        validateDelivering();
        this.status = DeliveryManagerStatus.AVAILABLE;
    }

    public void deactivate() {
        this.active = false;
    }

    public void reactivate() {
        this.active = true;
    }

    public void assignToDelivery() {
        if(!active){
            throw new BusinessException(
                    ErrorCode.DELIVERY_MANAGER_NOT_ACTIVE
            );
        }

        if (this.status != DeliveryManagerStatus.AVAILABLE) {
            throw new BusinessException(
                    ErrorCode.DELIVERY_MANAGER_NOT_AVAILABLE
            );
        }

        this.status = DeliveryManagerStatus.DELIVERING;
    }

    private static void validateHubId(DeliveryManagerType type, UUID hubId){
        if(type == DeliveryManagerType.HUB_DELIVERY && hubId != null){
            throw new BusinessException(
                    ErrorCode.INVALID_HUB_DELIVERY_MANAGER
            );
        }

        if(type == DeliveryManagerType.COMPANY_DELIVERY && hubId == null){
            throw new BusinessException(
                    ErrorCode.INVALID_COMPANY_DELIVERY_MANAGER
            );
        }
    }

    private void validateNotDelivering(){
        if(this.status == DeliveryManagerStatus.DELIVERING){
            throw new BusinessException(
                    ErrorCode.DELIVERY_MANAGER_IS_DELIVERING
            );
        }
    }

    private void validateDelivering() {
        if (this.status != DeliveryManagerStatus.DELIVERING) {
            throw new BusinessException(
                    ErrorCode.DELIVERY_MANAGER_NOT_DELIVERING
            );
        }
    }
}