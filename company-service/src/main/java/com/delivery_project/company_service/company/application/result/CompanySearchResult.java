package com.delivery_project.company_service.company.application.result;

import com.delivery_project.company_service.company.domain.entity.Company;
import org.springframework.data.domain.Page;

import java.util.List;

public record CompanySearchResult(

        List<CompanySearchDataResult> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static CompanySearchResult from(Page<Company> page) {
        return new CompanySearchResult(
                page.getContent()
                        .stream()
                        .map(CompanySearchDataResult::from)
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
