package com.delivery_project.company_service.company.infrastructure.persistence;

import com.delivery_project.company_service.company.domain.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataProductRepository extends JpaRepository<Product, UUID> {
}
