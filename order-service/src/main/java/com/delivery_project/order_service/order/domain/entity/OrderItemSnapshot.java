package com.delivery_project.order_service.order.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * 이력 한 건에 속한 상품 줄. 컬럼 구성은 팀문서 p_order_item_snapshots 명세를 따른다.
 *
 * <p>TODO product_name 은 팀문서상 NOT NULL 이지만 company-service 연동 전에는
 * 상품명을 알 수 없어 nullable 로 둔다. 연동 시 {@link #fillProductName} 으로 채운다.
 */
@Entity
@Getter
@Table(name = "p_order_item_snapshots",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_snapshot_product", columnNames = {"snapshot_id", "product_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItemSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private OrderSnapshot snapshot;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    /** ⭐ 당시 상품명. 상품이 삭제·개명돼도 여기 남는다 */
    @Column(name = "product_name", length = 100)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    private OrderItemSnapshot(UUID productId, String productName, Integer quantity) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.createdAt = Instant.now();
    }

    void assignSnapshot(OrderSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    /** company 연동 단계에서 상품명을 채운다. 이미 채워진 값은 덮어쓰지 않는다 */
    public void fillProductName(String productName) {
        if (this.productName == null) {
            this.productName = productName;
        }
    }
}
