package com.delivery_project.company_service.company.infrastructure.persistence;

import com.delivery_project.company_service.company.domain.entity.Product;
import com.delivery_project.company_service.company.domain.repository.ProductCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProductCommandRepositoryImpl implements ProductCommandRepository {

    private final SpringDataProductRepository springDataProductRepository;

    @Override
    public Product save(Product product) {
        return springDataProductRepository.save(product);
    }

    @Override
    public Optional<Product> findById(UUID productId) {
        return springDataProductRepository.findById(productId);
    }
}
