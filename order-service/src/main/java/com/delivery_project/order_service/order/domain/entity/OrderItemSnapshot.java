package com.delivery_project.order_service.order.domain.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Table(name = "p_order_item_snapshots",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_snapshot_line", columnNames = {"snapshot_id", "line_no"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItemSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private OrderSnapshot snapshot;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    /** ⭐ 당시 상품명. 상품이 삭제돼도 여기 남는다 */
    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "line_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal linePrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    private OrderItemSnapshot(UUID productId, String productName,
                              BigDecimal unitPrice, Integer quantity, BigDecimal linePrice) {
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.linePrice = linePrice;
        this.createdAt = Instant.now();
    }

    void assignSnapshot(OrderSnapshot snapshot, int lineNo) {
        this.snapshot = snapshot;
        this.lineNo = lineNo;
    }
}
