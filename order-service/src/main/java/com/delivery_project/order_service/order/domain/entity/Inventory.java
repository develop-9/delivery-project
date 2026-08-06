package com.delivery_project.order_service.order.domain.entity;

import com.delivery_project.order_service.global.common.BaseDeletableEntity;
import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 재고. 주문의 선점/확정 차감 대상이다.
 *
 * ⚠️ 1단계(주문 CRUD + Search)에서는 아직 어떤 서비스도 이 엔티티를 건드리지 않는다.
 *    재고 선점 연동 단계에서 InventoryCommandService / OrderCommandService 가 사용한다.
 */
@Entity
@Getter
@Table(name = "p_inventories",
        indexes = @Index(name = "idx_inventories_product_hub", columnList = "product_id, hub_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inventory extends BaseDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "hub_id", nullable = false)
    private UUID hubId;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    /** 창고에 실제로 있는 개수 */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /** 진행 중 주문이 찜해둔 개수 */
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    /**
     * 낙관적 락 버전.
     * 두 요청이 동시에 이 행을 수정하면 나중 커밋이 예외로 튕긴다.
     */
    @Version
    @Column(name = "version")
    private Long version;

    @Builder
    private Inventory(UUID productId, UUID hubId, UUID companyId, Integer quantity) {
        this.productId = productId;
        this.hubId = hubId;
        this.companyId = companyId;
        this.quantity = quantity != null ? quantity : 0;
        this.reservedQuantity = 0;
    }

    /** 주문 가능한 개수 */
    public int getAvailableQuantity() {
        return quantity - reservedQuantity;
    }

    // ────────── 재고 상태 전이 ──────────

    /** ① 선점 (주문 접수) — 실물은 그대로, 예약만 늘린다 */
    public void reserve(int qty, String productName) {
        if (getAvailableQuantity() < qty) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                    String.format("%s의 재고가 부족합니다. (요청: %d, 가용: %d)",
                            productName, qty, getAvailableQuantity()));
        }
        this.reservedQuantity += qty;
    }

    /** ② 선점 해제 (취소·실패) — 실물은 그대로 */
    public void release(int qty) {
        this.reservedQuantity = Math.max(0, this.reservedQuantity - qty);
    }

    /** ③ 확정 차감 (배송 완료) ⭐ 실물이 줄어드는 유일한 지점 */
    public void confirm(int qty) {
        if (this.reservedQuantity < qty || this.quantity < qty) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS,
                    "확정 차감할 선점 수량이 부족합니다.");
        }
        this.quantity -= qty;
        this.reservedQuantity -= qty;
    }

    /** ④ 입고 — 누적 */
    public void inbound(int qty) {
        if (qty < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "입고 수량은 1 이상이어야 합니다.");
        }
        this.quantity += qty;
    }

    /** 실사 보정 — 덮어쓰기. 선점 수량 아래로는 못 내린다 */
    public void adjust(int newQuantity, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "보정 사유는 필수입니다.");
        }
        if (newQuantity < this.reservedQuantity) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                    String.format("선점된 수량(%d)보다 적게 보정할 수 없습니다.", reservedQuantity));
        }
        this.quantity = newQuantity;
    }

    /** 삭제 가능 여부 — 진행 중 주문이 선점한 재고가 있으면 못 지운다 */
    public void validateDeletable() {
        if (this.reservedQuantity > 0) {
            throw new BusinessException(ErrorCode.INVENTORY_IN_USE,
                    "진행 중인 주문이 선점한 재고가 있어 삭제할 수 없습니다.");
        }
    }
}
