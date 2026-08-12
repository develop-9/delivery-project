package com.delivery_project.company_service.company.domain.repository;

import com.delivery_project.company_service.company.domain.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductQueryRepository {

    Optional<Product> findById(UUID productId);

    List<Product> findByCompanyId(UUID companyId);

    Page<Product> search(UUID companyId, String name, Integer minPrice, Integer maxPrice, Pageable pageable);
}
