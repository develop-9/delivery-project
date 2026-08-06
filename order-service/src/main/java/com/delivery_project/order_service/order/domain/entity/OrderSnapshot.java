package com.delivery_project.order_service.order.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 주문의 "그 순간"을 통째로 복사해 둔 이력 한 건.
 * 컬럼 구성은 팀문서 p_order_snapshots 명세를 따른다.
 *
 * <p>ID 가 아니라 <b>이름</b>을 복제 저장한다. 업체·허브가 개명·삭제돼도 이력은 그대로 남아야 한다.
 * 상품 줄은 p_order_item_snapshots 가 갖는다.
 *
 * <p>TODO 이름 컬럼은 팀문서상 NOT NULL 이지만, company/hub 연동 전에는 값을 알 수 없어
 * nullable 로 둔다. 연동 시 {@link #fillNames} 로 채우고 NOT NULL 로 조인다.
 */
@Entity
@Getter
@Table(name = "p_order_snapshots",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_order_sequence", columnNames = {"order_id", "sequence"}),
        indexes = @Index(name = "idx_snapshots_order", columnList = "order_id, sequence"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    /** 주문 내 이력 순번 (1부터) */
    @Column(name = "sequence", nullable = false)
    private Integer sequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 30)
    private OrderStatus orderStatus;

    @Column(name = "supplier_company_name", length = 100)
    private String supplierCompanyName;

    @Column(name = "receiver_company_name", length = 100)
    private String receiverCompanyName;

    @Column(name = "origin_hub_name", length = 100)
    private String originHubName;

    @Column(name = "dest_hub_name", length = 100)
    private String destHubName;

    @Column(name = "request_details", columnDefinition = "TEXT")
    private String requestDetails;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private UUID deletedBy;

    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemSnapshot> items = new ArrayList<>();

    private OrderSnapshot(Order order, int sequence, EventType eventType, UUID createdBy) {
        this.orderId = order.getId();
        this.sequence = sequence;
        this.eventType = eventType;
        this.orderStatus = order.getStatus();
        this.requestDetails = order.getRequestDetails();
        this.createdAt = Instant.now();
        this.createdBy = createdBy;
    }

    /**
     * 주문 현재 상태를 통째로 복사해 스냅샷을 만든다.
     * 안 바뀐 줄도 전부 복사한다.
     */
    public static OrderSnapshot capture(Order order, int sequence, EventType eventType, UUID createdBy) {
        OrderSnapshot snapshot = new OrderSnapshot(order, sequence, eventType, createdBy);

        for (OrderItem item : order.getItems()) {
            snapshot.addItem(OrderItemSnapshot.builder()
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .build());
        }

        return snapshot;
    }

    public void addItem(OrderItemSnapshot item) {
        item.assignSnapshot(this);
        items.add(item);
    }

    /**
     * company/hub 연동 단계에서 이름을 채워 넣는다.
     * 이미 채워진 값은 덮어쓰지 않는다 — 스냅샷은 당시 값이 진실이다.
     */
    public void fillNames(String supplierCompanyName, String receiverCompanyName,
                          String originHubName, String destHubName) {
        if (this.supplierCompanyName == null) {
            this.supplierCompanyName = supplierCompanyName;
        }
        if (this.receiverCompanyName == null) {
            this.receiverCompanyName = receiverCompanyName;
        }
        if (this.originHubName == null) {
            this.originHubName = originHubName;
        }
        if (this.destHubName == null) {
            this.destHubName = destHubName;
        }
    }

    public void softDelete(UUID userId) {
        if (isDeleted()) {
            return;
        }
        this.deletedAt = Instant.now();
        this.deletedBy = userId;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
