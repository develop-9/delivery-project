package com.delivery_project.delivery_service.delivery.domain.entity;

import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerStatus;
import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;
import com.delivery_project.delivery_service.global.common.BaseDeletableEntity;
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

    private static void validateHubId(DeliveryManagerType type, UUID hubId){
        if(type == DeliveryManagerType.HUB_DELIVERY && hubId != null){
            throw new IllegalArgumentException(
                    "허브 배송 담당자는 소속 허브를 가질 수 없습니다."
            );
        }

        if(type == DeliveryManagerType.COMPANY_DELIVERY && hubId == null){
            throw new IllegalArgumentException(
                    "업체 배송 담당자는 소속 허브가 필요합니다"
            );
        }
    }
}