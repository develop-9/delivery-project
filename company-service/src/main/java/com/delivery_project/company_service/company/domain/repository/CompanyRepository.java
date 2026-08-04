package com.delivery_project.company_service.company.domain.repository;

import com.delivery_project.company_service.company.domain.entity.Company;

public interface CompanyRepository {

    Company save(Company company);
}
