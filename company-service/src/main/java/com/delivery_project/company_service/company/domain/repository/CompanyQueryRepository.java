package com.delivery_project.company_service.company.domain.repository;

import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.entity.CompanyType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface CompanyQueryRepository {

    Optional<Company> findById(UUID companyId);

    Page<Company> search(String name, CompanyType type, UUID hubId, Pageable pageable);

    Boolean existsById(UUID companyId);
}
