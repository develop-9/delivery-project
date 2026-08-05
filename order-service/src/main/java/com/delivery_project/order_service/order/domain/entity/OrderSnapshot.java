package com.delivery_project.order_service.order.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 주문의 "그 순간"을 통째로 복사해 둔 이력 한 건.
 *
 * 바뀐 값만 저장하지 않고 전부 복사한다. 그래야 스냅샷 한 건만 봐도
 * 그때 주문이 어떤 구성이었는지 다른 행을 뒤지지 않고 알 수 있다.
 *
 * 업체·허브 "이름"은 company/hub 서비스 연동 전까지 비어 있고, ID 는 항상 채워진다.
 * 연동 후에는 이름까지 복사돼 업체가 개명·삭제돼도 이력은 그대로 남는다.
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

    @Column(name = "supplier_company_id", nullable = false)
    private UUID supplierCompanyId;

    @Column(name = "receiver_company_id", nullable = false)
    private UUID receiverCompanyId;

    @Column(name = "origin_hub_id", nullable = false)
    private UUID originHubId;

    @Column(name = "dest_hub_id", nullable = false)
    private UUID destHubId;

    // ⭐ ID 가 아니라 "이름"을 통째로 복사 저장한다.
    //    나중에 업체·허브가 삭제·개명돼도 이 값은 안 바뀐다.
    @Column(name = "supplier_company_name", length = 100)
    private String supplierCompanyName;

    @Column(name = "receiver_company_name", length = 100)
    private String receiverCompanyName;

    @Column(name = "origin_hub_name", length = 100)
    private String originHubName;

    @Column(name = "dest_hub_name", length = 100)
    private String destHubName;

    @Column(name = "delivery_id")
    private UUID deliveryId;

    @Column(name = "item_count", nullable = false)
    private Integer itemCount;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "total_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalPrice;

    @Column(name = "request_details", columnDefinition = "TEXT")
    private String requestDetails;

    @Column(name = "note", length = 255)
    private String note;

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

    private OrderSnapshot(Order order, int sequence, EventType eventType, String note, UUID createdBy) {
        this.orderId = order.getId();
        this.sequence = sequence;
        this.eventType = eventType;
        this.orderStatus = order.getStatus();
        this.supplierCompanyId = order.getSupplierCompanyId();
        this.receiverCompanyId = order.getReceiverCompanyId();
        this.originHubId = order.getOriginHubId();
        this.destHubId = order.getDestHubId();
        this.deliveryId = order.getDeliveryId();
        this.itemCount = order.getItemCount();
        this.totalQuantity = order.getTotalQuantity();
        this.totalPrice = order.getTotalPrice();
        this.requestDetails = order.getRequestDetails();
        this.note = note;
        this.createdAt = Instant.now();
        this.createdBy = createdBy;
    }

    /**
     * 주문 현재 상태를 통째로 복사해 스냅샷을 만든다.
     * 안 바뀐 줄도 전부 복사한다.
     */
    public static OrderSnapshot capture(Order order, int sequence, EventType eventType,
                                        String note, UUID createdBy) {
        OrderSnapshot snapshot = new OrderSnapshot(order, sequence, eventType, note, createdBy);

        for (OrderItem item : order.getItems()) {
            snapshot.addItem(OrderItemSnapshot.builder()
                    .productId(item.getProductId())
                    .productName(item.getProductName())
                    .unitPrice(item.getUnitPrice())
                    .quantity(item.getQuantity())
                    .linePrice(item.getLinePrice())
                    .build());
        }

        return snapshot;
    }

    /** line_no 를 자동 부여하며 줄을 담는다 */
    public void addItem(OrderItemSnapshot item) {
        item.assignSnapshot(this, items.size() + 1);
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
