package com.delivery_project.order_service.global.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PageableUtil {

    private static final Set<Integer> ALLOWED_SIZES = Set.of(10, 30, 50);
    private static final Set<String> DEFAULT_ALLOWED_SORTS = Set.of("createdAt", "updatedAt");
    private static final int DEFAULT_SIZE = 10;

    /** 주문 검색에서 허용하는 정렬 컬럼 (p_orders 가 실제로 가진 컬럼만) */
    public static final Set<String> ORDER_SORTS = Set.of("createdAt", "updatedAt");

    /** 주문 이력 조회에서 허용하는 정렬 컬럼 */
    public static final Set<String> SNAPSHOT_SORTS = Set.of("createdAt", "sequence");

    /** 재고 검색에서 추가로 허용하는 정렬 컬럼 */
    public static final Set<String> INVENTORY_SORTS = Set.of("createdAt", "updatedAt", "quantity", "reservedQuantity");

    private PageableUtil() {
    }

    public static Pageable normalize(Pageable pageable) {
        return normalize(pageable, DEFAULT_ALLOWED_SORTS);
    }

    /**
     * size 는 10/30/50 만 허용하고 그 외는 10 으로 고정한다.
     * sort 는 화이트리스트로 제한한다. 아무 컬럼이나 허용하면
     * 인덱스 없는 컬럼 정렬로 풀스캔이 발생한다.
     */
    public static Pageable normalize(Pageable pageable, Set<String> allowedSorts) {
        int size = ALLOWED_SIZES.contains(pageable.getPageSize())
                ? pageable.getPageSize() : DEFAULT_SIZE;

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        for (Sort.Order order : pageable.getSort()) {
            if (allowedSorts.contains(order.getProperty())) {
                sort = Sort.by(order.getDirection(), order.getProperty());
                break;
            }
        }

        return PageRequest.of(pageable.getPageNumber(), size, sort);
    }
}
