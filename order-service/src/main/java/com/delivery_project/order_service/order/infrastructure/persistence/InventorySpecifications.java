package com.delivery_project.order_service.order.infrastructure.persistence;

import com.delivery_project.order_service.order.domain.entity.Inventory;
import com.delivery_project.order_service.order.domain.repository.InventorySearchCondition;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class InventorySpecifications {

    private InventorySpecifications() {
    }

    public static Specification<Inventory> from(InventorySearchCondition condition) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isNull(root.get("deletedAt")));

            if (condition == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            addEquals(predicates, cb, root, "productId", condition.productId());
            addEquals(predicates, cb, root, "hubId", condition.hubId());
            addEquals(predicates, cb, root, "companyId", condition.companyId());

            if (condition.minQuantity() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("quantity"), condition.minQuantity()));
            }
            if (condition.maxQuantity() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("quantity"), condition.maxQuantity()));
            }

            // 가용 수량은 컬럼이 아니라 계산값이라 표현식으로 비교한다
            Expression<Integer> available = cb.diff(root.get("quantity"), root.get("reservedQuantity"));

            if (condition.minAvailableQuantity() != null) {
                predicates.add(cb.greaterThanOrEqualTo(available, condition.minAvailableQuantity()));
            }
            if (condition.maxAvailableQuantity() != null) {
                predicates.add(cb.lessThanOrEqualTo(available, condition.maxAvailableQuantity()));
            }
            if (Boolean.TRUE.equals(condition.onlyAvailable())) {
                predicates.add(cb.greaterThan(available, 0));
            }
            if (Boolean.TRUE.equals(condition.onlyReserved())) {
                predicates.add(cb.greaterThan(root.get("reservedQuantity"), 0));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static void addEquals(List<Predicate> predicates, CriteriaBuilder cb,
                                  Root<Inventory> root, String field, Object value) {
        if (value != null) {
            predicates.add(cb.equal(root.get(field), value));
        }
    }
}
