package com.delivery_project.company_service.company.domain.repository;

import com.delivery_project.company_service.company.domain.entity.Product;

import java.util.Optional;
import java.util.UUID;

public interface ProductCommandRepository {

    Product save(Product product);

    Optional<Product> findById(UUID productId);
}
