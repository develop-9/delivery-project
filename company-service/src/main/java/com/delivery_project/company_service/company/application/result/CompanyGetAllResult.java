package com.delivery_project.company_service.company.application.result;

import com.delivery_project.company_service.company.domain.entity.Company;
import org.springframework.data.domain.Page;

import java.util.List;

public record CompanyGetAllResult(

        List<CompanyGetAllDataResult> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static CompanyGetAllResult from(Page<Company> page) {
        return new CompanyGetAllResult(
                page.getContent()
                        .stream()
                        .map(CompanyGetAllDataResult::from)
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
