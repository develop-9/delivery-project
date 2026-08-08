package com.delivery_project.company_service.company.infrastructure.persistence;

import com.delivery_project.company_service.company.domain.entity.Product;
import com.delivery_project.company_service.global.config.JpaConfig;
import com.delivery_project.company_service.global.config.QueryDslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Import({
        ProductQueryRepositoryImpl.class,
        QueryDslConfig.class,
        JpaConfig.class
})
public class ProductQueryRepositorySearchTest {

    @Autowired
    private ProductQueryRepositoryImpl productQueryRepository;

    @Autowired
    private SpringDataProductRepository springDataProductRepository;

    @Test
    @DisplayName("조건에 맞는 상품을 검색한다.")
    void search_success() {
        // Given
        UUID companyId = UUID.randomUUID();

        Product product1 = Product.builder()
                .companyId(companyId)
                .name("테스트 상품 1")
                .price(10000)
                .build();

        Product product2 = Product.builder()
                .companyId(companyId)
                .name("테스트 상품 2")
                .price(20000)
                .build();

        Product otherProduct = Product.builder()
                .companyId(UUID.randomUUID())
                .name("다른 상품")
                .price(30000)
                .build();

        springDataProductRepository.saveAll(
                List.of(product1, product2, otherProduct)
        );

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // When
        Page<Product> result = productQueryRepository.search(
                companyId,
                "테스트",
                10000,
                20000,
                pageable
        );

        // Then
        assertThat(result.getTotalElements())
                .isEqualTo(2);

        assertThat(result.getContent())
                .hasSize(2)
                .allMatch(product ->
                        product.getCompanyId().equals(companyId)
                                && product.getName().contains("테스트")
                                && product.getPrice() >= 10000
                                && product.getPrice() <= 20000
                );
    }

    @Test
    @DisplayName("상품명 조건 없이 상품을 검색한다.")
    void search_success_withoutName() {
        // Given
        UUID companyId = UUID.randomUUID();

        Product product = Product.builder()
                .companyId(companyId)
                .name("테스트 상품")
                .price(10000)
                .build();

        springDataProductRepository.save(product);

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // When
        Page<Product> result = productQueryRepository.search(
                companyId,
                null,
                null,
                null,
                pageable
        );

        // Then
        assertThat(result.getTotalElements())
                .isEqualTo(1);

        assertThat(result.getContent())
                .containsExactly(product);
    }

    @Test
    @DisplayName("업체 ID로 상품을 검색한다.")
    void search_success_withCompanyId() {
        // Given
        UUID targetCompanyId = UUID.randomUUID();
        UUID otherCompanyId = UUID.randomUUID();

        Product targetProduct = Product.builder()
                .companyId(targetCompanyId)
                .name("대상 상품")
                .price(10000)
                .build();

        Product otherProduct = Product.builder()
                .companyId(otherCompanyId)
                .name("다른 상품")
                .price(20000)
                .build();

        springDataProductRepository.saveAll(
                List.of(targetProduct, otherProduct)
        );

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // When
        Page<Product> result = productQueryRepository.search(
                targetCompanyId,
                null,
                null,
                null,
                pageable
        );

        // Then
        assertThat(result.getTotalElements())
                .isEqualTo(1);

        assertThat(result.getContent())
                .containsExactly(targetProduct);
    }

    @Test
    @DisplayName("최소 가격 조건으로 상품을 검색한다.")
    void search_success_withMinPrice() {
        // Given
        UUID companyId = UUID.randomUUID();

        Product product1 = Product.builder()
                .companyId(companyId)
                .name("상품 1")
                .price(5000)
                .build();

        Product product2 = Product.builder()
                .companyId(companyId)
                .name("상품 2")
                .price(10000)
                .build();

        Product product3 = Product.builder()
                .companyId(companyId)
                .name("상품 3")
                .price(20000)
                .build();

        springDataProductRepository.saveAll(
                List.of(product1, product2, product3)
        );

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // When
        Page<Product> result = productQueryRepository.search(
                companyId,
                null,
                10000,
                null,
                pageable
        );

        // Then
        assertThat(result.getTotalElements())
                .isEqualTo(2);

        assertThat(result.getContent())
                .hasSize(2)
                .allMatch(product ->
                        product.getPrice() >= 10000
                );
    }

    @Test
    @DisplayName("최대 가격 조건으로 상품을 검색한다.")
    void search_success_withMaxPrice() {
        // Given
        UUID companyId = UUID.randomUUID();

        Product product1 = Product.builder()
                .companyId(companyId)
                .name("상품 1")
                .price(5000)
                .build();

        Product product2 = Product.builder()
                .companyId(companyId)
                .name("상품 2")
                .price(10000)
                .build();

        Product product3 = Product.builder()
                .companyId(companyId)
                .name("상품 3")
                .price(20000)
                .build();

        springDataProductRepository.saveAll(
                List.of(product1, product2, product3)
        );

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // When
        Page<Product> result = productQueryRepository.search(
                companyId,
                null,
                null,
                10000,
                pageable
        );

        // Then
        assertThat(result.getTotalElements())
                .isEqualTo(2);

        assertThat(result.getContent())
                .hasSize(2)
                .allMatch(product ->
                        product.getPrice() <= 10000
                );
    }

    @Test
    @DisplayName("최소 가격과 최대 가격 조건으로 상품을 검색한다.")
    void search_success_withPriceRange() {
        // Given
        UUID companyId = UUID.randomUUID();

        Product product1 = Product.builder()
                .companyId(companyId)
                .name("상품 1")
                .price(5000)
                .build();

        Product product2 = Product.builder()
                .companyId(companyId)
                .name("상품 2")
                .price(10000)
                .build();

        Product product3 = Product.builder()
                .companyId(companyId)
                .name("상품 3")
                .price(15000)
                .build();

        Product product4 = Product.builder()
                .companyId(companyId)
                .name("상품 4")
                .price(20000)
                .build();

        springDataProductRepository.saveAll(
                List.of(product1, product2, product3, product4)
        );

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // When
        Page<Product> result = productQueryRepository.search(
                companyId,
                null,
                10000,
                15000,
                pageable
        );

        // Then
        assertThat(result.getTotalElements())
                .isEqualTo(2);

        assertThat(result.getContent())
                .hasSize(2)
                .allMatch(product ->
                        product.getPrice() >= 10000
                                && product.getPrice() <= 15000
                );
    }

    @Test
    @DisplayName("검색 결과가 없으면 빈 Page를 반환한다.")
    void search_empty() {
        // Given
        UUID companyId = UUID.randomUUID();

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // When
        Page<Product> result = productQueryRepository.search(
                companyId,
                "존재하지않는상품",
                null,
                null,
                pageable
        );

        // Then
        assertThat(result)
                .isNotNull();

        assertThat(result.getContent())
                .isEmpty();

        assertThat(result.getTotalElements())
                .isZero();

        assertThat(result.getNumber())
                .isZero();

        assertThat(result.getSize())
                .isEqualTo(10);
    }

    @Test
    @DisplayName("페이지 크기에 맞게 상품을 조회한다.")
    void search_success_withPagination() {
        // Given
        UUID companyId = UUID.randomUUID();

        for (int i = 0; i < 5; i++) {
            springDataProductRepository.save(
                    Product.builder()
                            .companyId(companyId)
                            .name("테스트 상품 " + i)
                            .price(10000 + (i * 1000))
                            .build()
            );
        }

        Pageable pageable = PageRequest.of(
                0,
                2,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        // When
        Page<Product> result = productQueryRepository.search(
                companyId,
                null,
                null,
                null,
                pageable
        );

        // Then
        assertThat(result.getTotalElements())
                .isEqualTo(5);

        assertThat(result.getContent())
                .hasSize(2);

        assertThat(result.getNumber())
                .isZero();

        assertThat(result.getSize())
                .isEqualTo(2);

        assertThat(result.getTotalPages())
                .isEqualTo(3);
    }

    @Test
    @DisplayName("createdAt 오름차순으로 상품을 조회한다.")
    void search_success_withAscendingSort() {
        // Given
        UUID companyId = UUID.randomUUID();

        Product product1 = Product.builder()
                .companyId(companyId)
                .name("첫 번째 상품")
                .price(10000)
                .build();

        Product product2 = Product.builder()
                .companyId(companyId)
                .name("두 번째 상품")
                .price(20000)
                .build();

        springDataProductRepository.save(product1);
        springDataProductRepository.flush();

        springDataProductRepository.save(product2);
        springDataProductRepository.flush();

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Direction.ASC, "createdAt")
        );

        // When
        Page<Product> result = productQueryRepository.search(
                companyId,
                null,
                null,
                null,
                pageable
        );

        // Then
        assertThat(result.getContent())
                .hasSize(2);

        assertThat(result.getContent().get(0).getId())
                .isEqualTo(product1.getId());

        assertThat(result.getContent().get(1).getId())
                .isEqualTo(product2.getId());
    }
}
