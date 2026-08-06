package com.delivery_project.company_service.company.infrastructure.persistence;

import com.delivery_project.company_service.company.domain.entity.Company;
import com.delivery_project.company_service.company.domain.entity.CompanyType;
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
        CompanyQueryRepositoryImpl.class,
        QueryDslConfig.class,
        JpaConfig.class
})
class CompanyQueryRepositorySearchTest {

    @Autowired
    private CompanyQueryRepositoryImpl companyQueryRepository;

    @Autowired
    private SpringDataCompanyRepository springDataCompanyRepository;

    @Test
    @DisplayName("조건에 맞는 업체를 검색한다.")
    void search_success() {
        // Given
        UUID hubId = UUID.randomUUID();

        Company company1 = Company.builder()
                .hubId(hubId)
                .type(CompanyType.PRODUCER)
                .name("테스트 생산 업체")
                .address("서울특별시 강남구")
                .build();

        Company company2 = Company.builder()
                .hubId(hubId)
                .type(CompanyType.PRODUCER)
                .name("테스트 제조 업체")
                .address("서울특별시 서초구")
                .build();

        Company otherCompany = Company.builder()
                .hubId(UUID.randomUUID())
                .type(CompanyType.RECEIVER)
                .name("다른 업체")
                .address("서울특별시 송파구")
                .build();

        springDataCompanyRepository.saveAll(
                List.of(company1, company2, otherCompany)
        );

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );

        // When
        Page<Company> result =
                companyQueryRepository.search(
                        "테스트",
                        CompanyType.PRODUCER,
                        hubId,
                        pageable
                );

        // Then
        assertThat(result.getTotalElements())
                .isEqualTo(2);

        assertThat(result.getContent())
                .hasSize(2)
                .allMatch(company ->
                        company.getHubId().equals(hubId)
                                && company.getType() == CompanyType.PRODUCER
                                && company.getName().contains("테스트")
                );
    }

    @Test
    @DisplayName("이름 조건 없이 업체를 검색한다.")
    void search_success_withoutName() {
        // Given
        UUID hubId = UUID.randomUUID();

        Company company = Company.builder()
                .hubId(hubId)
                .type(CompanyType.PRODUCER)
                .name("테스트 생산 업체")
                .address("서울특별시 강남구")
                .build();

        springDataCompanyRepository.save(company);

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );

        // When
        Page<Company> result =
                companyQueryRepository.search(
                        null,
                        CompanyType.PRODUCER,
                        hubId,
                        pageable
                );

        // Then
        assertThat(result.getTotalElements())
                .isEqualTo(1);

        assertThat(result.getContent())
                .containsExactly(company);
    }

    @Test
    @DisplayName("업체 유형으로 업체를 검색한다.")
    void search_success_withType() {
        // Given
        UUID hubId = UUID.randomUUID();

        Company producer = Company.builder()
                .hubId(hubId)
                .type(CompanyType.PRODUCER)
                .name("생산 업체")
                .address("서울특별시 강남구")
                .build();

        Company receiver = Company.builder()
                .hubId(hubId)
                .type(CompanyType.RECEIVER)
                .name("수령 업체")
                .address("서울특별시 강남구")
                .build();

        springDataCompanyRepository.saveAll(
                List.of(producer, receiver)
        );

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );

        // When
        Page<Company> result =
                companyQueryRepository.search(
                        null,
                        CompanyType.PRODUCER,
                        null,
                        pageable
                );

        // Then
        assertThat(result.getTotalElements())
                .isEqualTo(1);

        assertThat(result.getContent())
                .containsExactly(producer);
    }

    @Test
    @DisplayName("허브 ID로 업체를 검색한다.")
    void search_success_withHubId() {
        // Given
        UUID targetHubId = UUID.randomUUID();
        UUID otherHubId = UUID.randomUUID();

        Company targetCompany = Company.builder()
                .hubId(targetHubId)
                .type(CompanyType.PRODUCER)
                .name("대상 업체")
                .address("서울특별시 강남구")
                .build();

        Company otherCompany = Company.builder()
                .hubId(otherHubId)
                .type(CompanyType.PRODUCER)
                .name("다른 업체")
                .address("서울특별시 강남구")
                .build();

        springDataCompanyRepository.saveAll(
                List.of(targetCompany, otherCompany)
        );

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );

        // When
        Page<Company> result =
                companyQueryRepository.search(
                        null,
                        null,
                        targetHubId,
                        pageable
                );

        // Then
        assertThat(result.getTotalElements())
                .isEqualTo(1);

        assertThat(result.getContent())
                .containsExactly(targetCompany);
    }

    @Test
    @DisplayName("검색 결과가 없으면 빈 Page를 반환한다.")
    void search_empty() {
        // Given
        UUID hubId = UUID.randomUUID();

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );

        // When
        Page<Company> result =
                companyQueryRepository.search(
                        "존재하지않는업체",
                        CompanyType.PRODUCER,
                        hubId,
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
    @DisplayName("페이지 크기에 맞게 업체를 조회한다.")
    void search_success_withPagination() {
        // Given
        UUID hubId = UUID.randomUUID();

        for (int i = 0; i < 5; i++) {
            springDataCompanyRepository.save(
                    Company.builder()
                            .hubId(hubId)
                            .type(CompanyType.PRODUCER)
                            .name("테스트 업체 " + i)
                            .address("서울특별시 강남구")
                            .build()
            );
        }

        Pageable pageable = PageRequest.of(
                0,
                2,
                Sort.by(
                        Sort.Direction.DESC,
                        "createdAt"
                )
        );

        // When
        Page<Company> result =
                companyQueryRepository.search(
                        null,
                        CompanyType.PRODUCER,
                        hubId,
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
    @DisplayName("createdAt 오름차순으로 업체를 조회한다.")
    void search_success_withAscendingSort() {
        // Given
        UUID hubId = UUID.randomUUID();

        Company company1 = Company.builder()
                .hubId(hubId)
                .type(CompanyType.PRODUCER)
                .name("첫 번째 업체")
                .address("서울특별시 강남구")
                .build();

        Company company2 = Company.builder()
                .hubId(hubId)
                .type(CompanyType.PRODUCER)
                .name("두 번째 업체")
                .address("서울특별시 강남구")
                .build();

        springDataCompanyRepository.save(company1);
        springDataCompanyRepository.flush();

        springDataCompanyRepository.save(company2);
        springDataCompanyRepository.flush();

        Pageable pageable = PageRequest.of(
                0,
                10,
                Sort.by(
                        Sort.Direction.ASC,
                        "createdAt"
                )
        );

        // When
        Page<Company> result =
                companyQueryRepository.search(
                        null,
                        CompanyType.PRODUCER,
                        hubId,
                        pageable
                );

        // Then
        assertThat(result.getContent())
                .hasSize(2);

        assertThat(result.getContent().get(0).getId())
                .isEqualTo(company1.getId());

        assertThat(result.getContent().get(1).getId())
                .isEqualTo(company2.getId());
    }
}
