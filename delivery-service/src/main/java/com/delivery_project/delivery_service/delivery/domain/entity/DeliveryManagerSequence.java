package com.delivery_project.delivery_service.delivery.domain.entity;

import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryManagerType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "p_delivery_manager_sequences")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryManagerSequence {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "manager_type", nullable = false, length = 30)
    private DeliveryManagerType managerType;

    @Column(name = "hub_id")
    private UUID hubId;

    @Column(name = "next_sequence", nullable = false)
    private Integer nextSequence;

    @Column(name = "last_assigned_sequence", nullable = false)
    private Integer lastAssignedSequence;

    private DeliveryManagerSequence(
            DeliveryManagerType managerType,
            UUID hubId
    ){
        this.managerType = managerType;
        this.hubId = hubId;
        this.nextSequence = 0;
        this.lastAssignedSequence = -1;
    }

    public static DeliveryManagerSequence create(
            DeliveryManagerType managerType,
            UUID hubId
    ){
        return new DeliveryManagerSequence(
                managerType,
                resolveHubId(managerType, hubId)
        );
    }

    private static UUID resolveHubId(
            DeliveryManagerType managerType,
            UUID hubId
    ){
        if(managerType == DeliveryManagerType.HUB_DELIVERY){
            return null;
        }
        return hubId;
    }

    public int issueNextSequence(){ // → 신규 담당자 sequence 발급
        int issuedSequence = this.nextSequence;
        this.nextSequence++;

        return issuedSequence;
    }

    public void updateLastAssignedSequence( // → Round-Robin 마지막 배정 위치
            Integer sequence
    ){
        this.lastAssignedSequence = sequence;
    }
}
