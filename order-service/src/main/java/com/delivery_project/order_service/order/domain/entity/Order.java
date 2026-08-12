package com.delivery_project.order_service.order.domain.entity;

import com.delivery_project.order_service.global.common.BaseDeletableEntity;
import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 주문. 컬럼 구성은 팀문서 p_orders 명세를 그대로 따른다.
 *
 * <p>출발·도착 허브는 여기 저장하지 않는다. 출발 허브는 상품(p_products.hub_id),
 * 도착 허브는 수령 업체(p_companies.hub_id)에서 도출되므로 order 가 복제해 들고 있으면
 * 원본이 바뀔 때 어긋난다. 배송 ID 도 갖지 않는다 — 참조는 p_deliveries.order_id 단방향이다.
 */
@Entity
@Getter
@Table(
        name = "p_orders",
        indexes = {
                @Index(name = "idx_orders_status_created", columnList = "status, created_at"),
                @Index(name = "idx_orders_supplier", columnList = "supplier_company_id, status"),
                @Index(name = "idx_orders_receiver", columnList = "receiver_company_id, status"),
                @Index(name = "idx_orders_receiver_user", columnList = "receiver_user_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** 생산·공급 업체 */
    @Column(name = "supplier_company_id", nullable = false)
    private UUID supplierCompanyId;

    /** 수령 업체 */
    @Column(name = "receiver_company_id", nullable = false)
    private UUID receiverCompanyId;

    /** 주문 수신자 (논리 FK → p_users.id) */
    @Column(name = "receiver_user_id", nullable = false)
    private UUID receiverUserId;

    @Column(name = "request_details", columnDefinition = "TEXT")
    private String requestDetails;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status;

    /**
     * cascade = ALL       : 주문 저장 시 줄도 함께 저장
     * orphanRemoval = true: 리스트에서 빼면 DB 에서도 삭제 (줄은 물리 삭제)
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Builder
    private Order(UUID supplierCompanyId, UUID receiverCompanyId,
                  UUID receiverUserId, String requestDetails) {
        this.supplierCompanyId = supplierCompanyId;
        this.receiverCompanyId = receiverCompanyId;
        this.receiverUserId = receiverUserId;
        this.requestDetails = requestDetails;
        this.status = OrderStatus.PENDING;
    }

    // ────────── 줄 관리 ──────────

    public void addItem(OrderItem item) {
        boolean duplicated = items.stream()
                .anyMatch(i -> i.getProductId().equals(item.getProductId()));
        if (duplicated) {
            throw new BusinessException(ErrorCode.DUPLICATE_ORDER_ITEM);
        }

        items.add(item);
        item.assignOrder(this);
    }

    public void removeItem(UUID productId) {
        if (items.size() <= 1) {
            throw new BusinessException(ErrorCode.ORDER_ITEM_REQUIRED,
                    "주문 상품을 모두 삭제할 수 없습니다. 주문 취소를 이용해 주세요.");
        }

        boolean removed = items.removeIf(i -> i.getProductId().equals(productId));
        if (!removed) {
            throw new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND);
        }
    }

    public OrderItem findItem(UUID productId) {
        return items.stream()
                .filter(i -> i.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_ITEM_NOT_FOUND));
    }

    /**
     * 주문 줄을 요청 상태로 맞춘다.
     *
     * 전체 삭제 후 재삽입이 아니라 "있으면 수정 / 없으면 추가 / 빠진 건 제거" 로 병합한다.
     * (order_id, product_id) 유니크 제약이 걸려 있어, 같은 상품을 지웠다가 다시 넣으면
     * Hibernate 의 INSERT 가 DELETE 보다 먼저 나가 제약 위반이 날 수 있기 때문이다.
     */
    public void replaceItems(List<OrderItem> newItems) {
        if (newItems == null || newItems.isEmpty()) {
            throw new BusinessException(ErrorCode.ORDER_ITEM_REQUIRED);
        }

        Set<UUID> requestedProductIds = new HashSet<>();
        for (OrderItem item : newItems) {
            if (!requestedProductIds.add(item.getProductId())) {
                throw new BusinessException(ErrorCode.DUPLICATE_ORDER_ITEM,
                        "같은 상품을 여러 줄로 담을 수 없습니다. 수량을 합쳐서 요청해 주세요.");
            }
        }

        items.removeIf(existing -> !requestedProductIds.contains(existing.getProductId()));

        for (OrderItem newItem : newItems) {
            items.stream()
                    .filter(i -> i.getProductId().equals(newItem.getProductId()))
                    .findFirst()
                    .ifPresentOrElse(
                            existing -> existing.change(newItem.getQuantity(), newItem.getInventoryId()),
                            () -> {
                                newItem.assignOrder(this);
                                items.add(newItem);
                            });
        }
    }

    // ────────── 상태 전이 ──────────

    /** 배송·경로 생성이 끝났을 때 delivery-service 통보로 호출된다 */
    public void confirm() {
        validateTransit(OrderStatus.CONFIRMED);
        this.status = OrderStatus.CONFIRMED;
    }

    public void cancel() {
        validateTransit(OrderStatus.CANCELED);
        this.status = OrderStatus.CANCELED;
    }

    public void markFailed() {
        validateTransit(OrderStatus.FAILED);
        this.status = OrderStatus.FAILED;
    }

    private void validateTransit(OrderStatus next) {
        if (!this.status.canTransitTo(next)) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS,
                    String.format("허용되지 않는 상태 전이입니다. (%s → %s)", this.status, next));
        }
    }

    public void validateModifiable() {
        if (!this.status.isModifiable()) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS,
                    String.format("%s 상태의 주문은 변경할 수 없습니다.", this.status));
        }
    }

    /**
     * 배송이 이미 만들어진 주문은 줄 구성을 못 바꾼다.
     * 배송 생성 여부는 order 가 아니라 status(CONFIRMED)가 말해준다.
     */
    public void validateItemsModifiable() {
        validateModifiable();
        if (this.status == OrderStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.DELIVERY_ALREADY_STARTED,
                    "배송이 생성된 주문의 상품 구성은 변경할 수 없습니다.");
        }
    }

    public void updateDetails(String requestDetails) {
        if (requestDetails != null) {
            this.requestDetails = requestDetails;
        }
    }
}
