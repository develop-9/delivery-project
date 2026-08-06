package com.delivery_project.order_service.order.domain.entity;

import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * 주문 상품 줄. 컬럼 구성은 팀문서 p_order_items 명세를 따른다.
 *
 * <p>상품명·단가는 여기 두지 않는다. 원본은 company-service(p_products)이고,
 * "주문 당시 값"이 필요한 곳은 이력(p_order_item_snapshots)이다.
 */
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

    /** 논리 FK → p_products.id */
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** 이 줄이 선점한 재고 행 (실제 FK → p_inventories.id) */
    @Column(name = "inventory_id", nullable = false)
    private UUID inventoryId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    private OrderItem(UUID productId, Integer quantity, UUID inventoryId) {
        validate(quantity, inventoryId);

        this.productId = productId;
        this.quantity = quantity;
        this.inventoryId = inventoryId;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    void assignOrder(Order order) {
        this.order = order;
    }

    /** 주문 수정 — 수량과 선점 재고 행을 맞춘다 */
    void change(Integer quantity, UUID inventoryId) {
        validate(quantity, inventoryId);

        this.quantity = quantity;
        this.inventoryId = inventoryId;
        this.updatedAt = Instant.now();
    }

    /**
     * 수량 불변식은 엔티티가 스스로 지킨다.
     * DTO 검증만 믿으면 배치나 내부 API 로 값이 들어올 때 규칙이 새어 나간다.
     */
    private void validate(Integer quantity, UUID inventoryId) {
        if (quantity == null || quantity < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "수량은 1 이상이어야 합니다.");
        }
        if (inventoryId == null) {
            throw new BusinessException(ErrorCode.INVENTORY_NOT_FOUND, "선점할 재고를 찾을 수 없습니다.");
        }
    }
}
