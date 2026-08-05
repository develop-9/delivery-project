package com.delivery_project.company_service.company.application.support.pagination;

import com.delivery_project.company_service.global.exception.BusinessException;
import com.delivery_project.company_service.global.exception.ErrorCode;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class PageValidator {

    // page 검증
    public int validatePage(Integer page) {
        if (page == null || page < 0) {
            throw new BusinessException(ErrorCode.INVALID_PAGE);
        }

        return page;
    }

    // size 검증
    public int normalizeSize(Integer size) {
        if (size == null) {
            return 10;
        }

        return switch (size) {
            case 10, 30, 50 -> size;
            default -> 10;
        };
    }

    // 정렬 검증
    public Sort normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(
                    Sort.Direction.DESC,
                    "createdAt"
            );
        }

        return switch (sort) {
            case "createdAt,asc" ->
                    Sort.by(
                            Sort.Direction.ASC,
                            "createdAt"
                    );

            default ->
                    Sort.by(
                            Sort.Direction.DESC,
                            "createdAt"
                    );
        };
    }
}
