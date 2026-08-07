package com.delivery_project.delivery_service.delivery.domain.entity;

import com.delivery_project.delivery_service.delivery.domain.enums.DeliveryRouteStatus;
import com.delivery_project.delivery_service.global.common.BaseDeletableEntity;
import com.delivery_project.delivery_service.global.exception.BusinessException;
import com.delivery_project.delivery_service.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Entity
@Table(name = "p_delivery_routes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DeliveryRoute extends BaseDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "delivery_id", nullable = false)
    private UUID deliveryId;

    @Column(name = "sequence", nullable = false)
    private Integer sequence; // 이동 구간 순서

    @Column(name = "departure_hub_id", nullable = false)
    private UUID departureHubId; // 구간 출발 허브

    @Column(name = "arrival_hub_id", nullable = false)
    private UUID arrivalHubId; // 구간 도착 허브

    @Column(name = "estimated_distance_km",
            nullable = false,
            precision = 10,
            scale = 2)
    private BigDecimal estimatedDistanceKm; // 예상 거리

    @Column(name = "estimated_duration_min", nullable = false)
    private Integer estimatedDurationMin; // 예상 시간

    @Column(name = "actual_distance_km", precision = 10, scale = 2)
    private BigDecimal actualDistanceKm; // 실제 거리

    @Column(name = "actual_duration_min")
    private Integer actualDurationMin; // 실제 시간

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DeliveryRouteStatus status; // 구간 상태

    @Column(name = "delivery_manager_id")
    private UUID deliveryManagerId; // 허브 배송 담당자

    private DeliveryRoute(
            UUID deliveryId,
            Integer sequence,
            UUID departureHubId,
            UUID arrivalHubId,
            BigDecimal estimatedDistanceKm,
            Integer estimatedDurationMin
    ){
        this.deliveryId = deliveryId;
        this.sequence = sequence;
        this.departureHubId = departureHubId;
        this.arrivalHubId = arrivalHubId;
        this.estimatedDistanceKm = estimatedDistanceKm;
        this.estimatedDurationMin = estimatedDurationMin;

        this.status = DeliveryRouteStatus.WAITING;
    }

    public static DeliveryRoute create(
            UUID deliveryId,
            Integer sequence,
            UUID departureHubId,
            UUID arrivalHubId,
            BigDecimal estimatedDistanceKm,
            Integer estimatedDurationMin
    ) {
        return new DeliveryRoute(
                deliveryId,
                sequence,
                departureHubId,
                arrivalHubId,
                estimatedDistanceKm,
                estimatedDurationMin
        );
    }

    public void start(UUID deliveryManagerId){
        if(this.status != DeliveryRouteStatus.WAITING){
            throw new BusinessException(
                    ErrorCode.INVALID_ROUTE_STATUS_TRANSITION
            );
        }
        this.deliveryManagerId = deliveryManagerId;
        this.status = DeliveryRouteStatus.IN_TRANSIT;
    }

    public void arrive(
            BigDecimal actualDistanceKm,
            Integer actualDurationMin
    ) {
        if (this.status != DeliveryRouteStatus.IN_TRANSIT) {
            throw new BusinessException(
                    ErrorCode.INVALID_ROUTE_STATUS_TRANSITION
            );
        }

        this.actualDistanceKm = actualDistanceKm;
        this.actualDurationMin = actualDurationMin;
        this.status = DeliveryRouteStatus.ARRIVED;
    }
}
