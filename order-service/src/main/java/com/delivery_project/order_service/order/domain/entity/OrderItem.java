package com.delivery_project.order_service.order.domain.entity;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
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
@Table(name = "p_order_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_order_product", columnNames = {"order_id", "product_id"}),
        indexes = @Index(name = "idx_order_items_product", columnList = "product_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** LAZY 필수. EAGER 면 줄 조회마다 주문까지 딸려온다 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    /**
     * 주문 당시 상품명.
     * company-service 연동 전까지는 요청값을 그대로 받고,
     * 연동 후에는 상품 조회 결과로 덮어쓴다. 어느 쪽이든 주문 시점 값이 그대로 남는다.
     */
    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** 주문 당시 단가. 나중에 상품 가격이 올라도 안 바뀐다 */
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "line_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal linePrice;

    /** 이 줄이 선점한 재고 행. 재고 연동 단계에서 채워진다 */
    @Column(name = "inventory_id")
    private UUID inventoryId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    private OrderItem(UUID productId, String productName, Integer quantity,
                      BigDecimal unitPrice, UUID inventoryId) {
        validate(quantity, unitPrice);

        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.linePrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.inventoryId = inventoryId;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    void assignOrder(Order order) {
        this.order = order;
    }

    /** 주문 수정 — 수량·단가·상품명을 한 번에 맞춘다 */
    void change(String productName, int quantity, BigDecimal unitPrice) {
        validate(quantity, unitPrice);

        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.linePrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.updatedAt = Instant.now();
    }

    void assignInventory(UUID inventoryId) {
        this.inventoryId = inventoryId;
    }

    /**
     * 금액·수량 불변식은 엔티티가 스스로 지킨다.
     * DTO 검증만 믿으면 배치나 내부 API 로 값이 들어올 때 규칙이 새어 나간다.
     */
    private void validate(Integer quantity, BigDecimal unitPrice) {
        if (quantity == null || quantity < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "수량은 1 이상이어야 합니다.");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "단가는 0 이상이어야 합니다.");
        }
    }
}
