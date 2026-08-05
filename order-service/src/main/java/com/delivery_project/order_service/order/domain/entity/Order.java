package com.delivery_project.order_service.order.domain.entity;

import com.delivery_project.order_service.global.common.BaseDeletableEntity;
import com.delivery_project.order_service.global.exception.BusinessException;
import com.delivery_project.order_service.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Getter
@Table(
        name = "p_orders",
        indexes = {
                @Index(name = "idx_orders_status_created", columnList = "status, created_at"),
                @Index(name = "idx_orders_origin_hub", columnList = "origin_hub_id, status"),
                @Index(name = "idx_orders_receiver", columnList = "receiver_company_id, status"),
                @Index(name = "idx_orders_requester", columnList = "requester_user_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "supplier_company_id", nullable = false)
    private UUID supplierCompanyId;

    @Column(name = "receiver_company_id", nullable = false)
    private UUID receiverCompanyId;

    /** 출발 허브 (재고 선점 대상 — 재고 연동 단계에서 사용) */
    @Column(name = "origin_hub_id", nullable = false)
    private UUID originHubId;

    /** 도착 허브 (수령 업체 소속 허브) */
    @Column(name = "dest_hub_id", nullable = false)
    private UUID destHubId;

    /** 주문 요청자 — Gateway 가 넘겨준 X-User-Id */
    @Column(name = "requester_user_id", nullable = false)
    private UUID requesterUserId;

    @Column(name = "item_count", nullable = false)
    private Integer itemCount;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(name = "total_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrderStatus status;

    /** 배송 연동 단계에서 delivery-service 가 채워준다 */
    @Column(name = "delivery_id")
    private UUID deliveryId;

    @Column(name = "request_details", columnDefinition = "TEXT")
    private String requestDetails;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "dispatch_deadline_at")
    private LocalDateTime dispatchDeadlineAt;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    /**
     * 동시에 같은 주문을 수정하면 나중 커밋이 예외로 튕긴다.
     * 수량 변경과 취소가 겹쳐 집계 컬럼이 어긋나는 것을 막는다.
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * cascade = ALL       : 주문 저장 시 줄도 함께 저장
     * orphanRemoval = true: 리스트에서 빼면 DB 에서도 삭제 (줄은 물리 삭제)
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Builder
    private Order(UUID supplierCompanyId, UUID receiverCompanyId,
                  UUID originHubId, UUID destHubId, UUID requesterUserId,
                  String requestDetails, LocalDateTime dueAt) {
        this.supplierCompanyId = supplierCompanyId;
        this.receiverCompanyId = receiverCompanyId;
        this.originHubId = originHubId;
        this.destHubId = destHubId;
        this.requesterUserId = requesterUserId;
        this.requestDetails = requestDetails;
        this.dueAt = dueAt;
        this.status = OrderStatus.PENDING;
        this.itemCount = 0;
        this.totalQuantity = 0;
        this.totalPrice = BigDecimal.ZERO;
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
        recalculate();
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

        recalculate();
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
                            existing -> existing.change(newItem.getProductName(),
                                    newItem.getQuantity(), newItem.getUnitPrice()),
                            () -> {
                                newItem.assignOrder(this);
                                items.add(newItem);
                            });
        }

        recalculate();
    }

    // ────────── 상태 전이 ──────────

    public void confirmDelivery(UUID deliveryId) {
        validateTransit(OrderStatus.CONFIRMED);
        this.deliveryId = deliveryId;
        this.status = OrderStatus.CONFIRMED;
    }

    public void complete() {
        validateTransit(OrderStatus.COMPLETED);
        this.status = OrderStatus.COMPLETED;
    }

    public void cancel(String reason) {
        validateTransit(OrderStatus.CANCELED);
        this.status = OrderStatus.CANCELED;
        this.cancelReason = reason;
    }

    public void markFailed(String reason) {
        this.status = OrderStatus.FAILED;
        this.cancelReason = reason;
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

    /** 배송이 이미 시작된 주문은 줄 구성을 못 바꾼다 */
    public void validateItemsModifiable() {
        validateModifiable();
        if (this.deliveryId != null) {
            throw new BusinessException(ErrorCode.DELIVERY_ALREADY_STARTED,
                    "배송이 생성된 주문의 상품 구성은 변경할 수 없습니다.");
        }
    }

    public void updateDetails(String requestDetails, LocalDateTime dueAt) {
        if (requestDetails != null) {
            this.requestDetails = requestDetails;
        }
        if (dueAt != null) {
            this.dueAt = dueAt;
        }
    }

    public void updateDispatchDeadline(LocalDateTime deadline) {
        this.dispatchDeadlineAt = deadline;
    }

    /** 줄이 바뀔 때마다 집계 컬럼 재계산 */
    private void recalculate() {
        this.itemCount = items.size();
        this.totalQuantity = items.stream().mapToInt(OrderItem::getQuantity).sum();
        this.totalPrice = items.stream()
                .map(OrderItem::getLinePrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** 발송 시한이 지났는데 아직 안 나갔는지 */
    public boolean isDispatchOverdue() {
        return dispatchDeadlineAt != null
                && LocalDateTime.now().isAfter(dispatchDeadlineAt)
                && (status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED);
    }
}
