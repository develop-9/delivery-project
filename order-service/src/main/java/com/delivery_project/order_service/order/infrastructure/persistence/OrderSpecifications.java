package com.delivery_project.order_service.order.infrastructure.persistence;

import com.delivery_project.order_service.order.domain.entity.Order;
import com.delivery_project.order_service.order.domain.entity.OrderItem;
import com.delivery_project.order_service.order.domain.repository.OrderSearchCondition;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    public static Specification<Order> from(OrderSearchCondition condition) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 논리 삭제된 주문은 검색에서 항상 제외한다
            predicates.add(cb.isNull(root.get("deletedAt")));

            if (condition == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            if (condition.status() != null) {
                predicates.add(cb.equal(root.get("status"), condition.status()));
            }
            addEquals(predicates, cb, root, "supplierCompanyId", condition.supplierCompanyId());
            addEquals(predicates, cb, root, "receiverCompanyId", condition.receiverCompanyId());
            addEquals(predicates, cb, root, "receiverUserId", condition.receiverUserId());

            if (condition.createdFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"),
                        condition.createdFrom().atZone(ZoneId.systemDefault()).toInstant()));
            }
            if (condition.createdTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"),
                        condition.createdTo().atZone(ZoneId.systemDefault()).toInstant()));
            }

            // ⭐ 상품 조건은 join 이 아니라 EXISTS 서브쿼리로 건다.
            //    join 을 쓰면 줄 수만큼 주문이 중복돼 페이징의 totalElements 가 부풀어 오른다.
            if (condition.productId() != null) {
                Subquery<Long> sub = query.subquery(Long.class);
                Root<OrderItem> item = sub.from(OrderItem.class);
                sub.select(cb.literal(1L)).where(
                        cb.equal(item.get("order"), root),
                        cb.equal(item.get("productId"), condition.productId()));
                predicates.add(cb.exists(sub));
            }

            // 상품명은 order 가 소유하지 않으므로(company-service) 요청사항만 본다
            if (condition.hasKeyword()) {
                predicates.add(cb.like(cb.lower(root.get("requestDetails")), condition.keywordPattern()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addEquals(List<Predicate> predicates,
                                  jakarta.persistence.criteria.CriteriaBuilder cb,
                                  Root<Order> root, String field, Object value) {
        if (value != null) {
            predicates.add(cb.equal(root.get(field), value));
        }
    }
}
