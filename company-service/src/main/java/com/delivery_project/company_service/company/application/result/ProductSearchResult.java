package com.delivery_project.company_service.company.application.result;

import com.delivery_project.company_service.company.domain.entity.Product;
import org.springframework.data.domain.Page;

import java.util.List;

public record ProductSearchResult(

        List<ProductSearchDataResult> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static ProductSearchResult from(Page<Product> page) {
        return new ProductSearchResult(
                page.getContent()
                        .stream()
                        .map(ProductSearchDataResult::from)
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
