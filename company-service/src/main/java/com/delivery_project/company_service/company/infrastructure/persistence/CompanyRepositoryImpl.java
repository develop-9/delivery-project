package com.delivery_project.company_service.company.infrastructure.persistence;

import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.repository.CompanyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CompanyRepositoryImpl implements CompanyRepository {

    private final SpringDataCompanyRepository springDataCompanyRepository;

    @Override
    public Company save(Company company) {
        return springDataCompanyRepository.save(company);
    }

    @Override
    public Optional<Company> findById(UUID companyId) {
        return springDataCompanyRepository.findById(companyId);
    }
}
