package com.delivery_project.company_service.company.domain.repository;

import com.delivery_project.company_service.company.domain.entity.Company;

import java.util.Optional;
import java.util.UUID;

public interface CompanyCommandRepository {

    Company save(Company company);

    Optional<Company> findById(UUID id);
}
